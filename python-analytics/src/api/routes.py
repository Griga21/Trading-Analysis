"""
Запуск: python run_training.py SBER --horizon 15

Отдельный скрипт для быстрой проверки модели без API/FastAPI —
запускается напрямую в консоли, результат сразу печатается.
"""
import argparse
import sys
from typing import Optional

import pandas as pd
import numpy as np
import xgboost as xgb
from sklearn.metrics import accuracy_score, precision_score, recall_score
from services.database_service import DatabaseService


def calculate_indicators(df: pd.DataFrame) -> pd.DataFrame:
    """
    Считает все индикаторы поверх сырых свечей (open/high/low/close/volume).
    Дублирует логику AnalysisService — держим здесь самодостаточно,
    чтобы скрипт не зависел от структуры остального проекта.
    """
    df = df.copy()

    # SMA / EMA
    df['SMA_20'] = df['close'].rolling(window=20).mean()
    df['SMA_50'] = df['close'].rolling(window=50).mean()
    df['EMA_20'] = df['close'].ewm(span=20, adjust=False).mean()

    # RSI
    delta = df['close'].diff()
    gain = (delta.where(delta > 0, 0)).rolling(window=14).mean()
    loss = (-delta.where(delta < 0, 0)).rolling(window=14).mean()
    rs = gain / loss.replace(0, np.nan)
    df['RSI'] = 100 - (100 / (1 + rs))
    df['RSI'] = df['RSI'].fillna(100)

    # Bollinger Bands
    df['BB_middle'] = df['close'].rolling(window=20).mean()
    bb_std = df['close'].rolling(window=20).std()
    df['BB_upper'] = df['BB_middle'] + (bb_std * 2)
    df['BB_lower'] = df['BB_middle'] - (bb_std * 2)

    # MACD
    ema_12 = df['close'].ewm(span=12, adjust=False).mean()
    ema_26 = df['close'].ewm(span=26, adjust=False).mean()
    df['MACD'] = ema_12 - ema_26
    df['MACD_signal'] = df['MACD'].ewm(span=9, adjust=False).mean()
    df['MACD_histogram'] = df['MACD'] - df['MACD_signal']

    # волатильность
    df['returns'] = df['close'].pct_change()
    df['volatility_20'] = df['returns'].rolling(window=20).std()

    return df


def get_candles_with_indicators(db_service: DatabaseService, security: str,
                                 start_date: Optional[str] = None,
                                 end_date: Optional[str] = None) -> pd.DataFrame:
    """
    Забирает сырые свечи из БД и считает поверх них индикаторы.
    Подставь сюда реальный метод твоего DatabaseService для получения свечей,
    если он называется иначе, чем get_candles.
    """
    df = db_service.get_candles(security, start_date, end_date)
    if df is None or df.empty:
        return pd.DataFrame()
    return calculate_indicators(df)


def build_feature_matrix(df: pd.DataFrame) -> pd.DataFrame:
    df = df.copy()

    feature_cols = [
        'SMA_20', 'SMA_50', 'EMA_20',
        'RSI', 'MACD', 'MACD_signal', 'MACD_histogram',
        'BB_upper', 'BB_lower', 'BB_middle',
        'volatility_20',
    ]

    df['price_to_sma20'] = df['close'] / df['SMA_20'] - 1
    df['price_to_sma50'] = df['close'] / df['SMA_50'] - 1
    df['sma20_to_sma50'] = df['SMA_20'] / df['SMA_50'] - 1
    df['bb_position'] = (df['close'] - df['BB_lower']) / (df['BB_upper'] - df['BB_lower'])

    engineered_cols = ['price_to_sma20', 'price_to_sma50', 'sma20_to_sma50', 'bb_position']

    all_features = feature_cols + engineered_cols
    return df[all_features + ['close']].copy()


def create_target(df: pd.DataFrame, horizon: int = 15, threshold: float = 0.0) -> pd.Series:
    future_return = df['close'].shift(-horizon) / df['close'] - 1
    return (future_return > threshold).astype(int)


def walk_forward_splits(df: pd.DataFrame, n_splits: int = 5, test_size: int = 120):
    n = len(df)
    splits = []
    for i in range(n_splits):
        test_end = n - (n_splits - i - 1) * test_size
        test_start = test_end - test_size
        train_end = test_start

        if train_end < 100:
            continue

        train_idx = list(range(0, train_end))
        test_idx = list(range(test_start, test_end))
        splits.append((train_idx, test_idx))

    return splits


def train_and_evaluate(df: pd.DataFrame, horizon: int = 15) -> dict:
    features = build_feature_matrix(df)
    target = create_target(df, horizon=horizon)

    data = features.join(target.rename('target')).dropna()
    X = data.drop(columns=['close', 'target'])
    y = data['target']

    splits = walk_forward_splits(data, n_splits=5, test_size=120)
    if not splits:
        raise ValueError("Недостаточно данных для walk-forward split — нужно больше истории")

    fold_results = []
    model: Optional[xgb.XGBClassifier] = None

    for fold, (train_idx, test_idx) in enumerate(splits):
        X_train, y_train = X.iloc[train_idx], y.iloc[train_idx]
        X_test, y_test = X.iloc[test_idx], y.iloc[test_idx]

        fold_model = xgb.XGBClassifier(
            n_estimators=100, max_depth=3, learning_rate=0.05,
            eval_metric='logloss'
        )
        fold_model.fit(X_train, y_train)
        preds = fold_model.predict(X_test)
        model = fold_model

        naive_preds = np.ones_like(y_test)

        fold_results.append({
            'fold': fold,
            'accuracy': accuracy_score(y_test, preds),
            'precision': precision_score(y_test, preds, zero_division=0),
            'recall': recall_score(y_test, preds, zero_division=0),
            'naive_accuracy': accuracy_score(y_test, naive_preds),
            'test_size': len(y_test),
        })

    assert model is not None, "Цикл walk-forward не выполнился ни разу — проверь splits"

    importance_df = pd.DataFrame({
        'feature': X.columns,
        'importance': model.feature_importances_
    }).sort_values('importance', ascending=False)

    return {
        'results_by_fold': fold_results,
        'feature_importance': importance_df,
        'model': model,
    }


def main():
    parser = argparse.ArgumentParser(description="Обучение и оценка baseline-модели по бумаге")
    parser.add_argument('security', type=str, help='Тикер, например SBER')
    parser.add_argument('--horizon', type=int, default=15, help='Горизонт прогноза в днях')
    args = parser.parse_args()

    print(f"Загружаю данные для {args.security}...")
    db_service = DatabaseService()
    df = get_candles_with_indicators(db_service, args.security)

    if df is None or df.empty:
        print(f"Нет данных для {args.security}", file=sys.stderr)
        sys.exit(1)

    print(f"Загружено {len(df)} строк. Запускаю обучение (horizon={args.horizon})...\n")

    try:
        result = train_and_evaluate(df, horizon=args.horizon)
    except ValueError as e:
        print(f"Ошибка: {e}", file=sys.stderr)
        sys.exit(1)

    print("=== Результаты по фолдам ===")
    print(pd.DataFrame(result['results_by_fold']).to_string(index=False))

    print("\n=== Feature importance ===")
    print(result['feature_importance'].to_string(index=False))

    avg_acc = pd.DataFrame(result['results_by_fold'])['accuracy'].mean()
    avg_naive = pd.DataFrame(result['results_by_fold'])['naive_accuracy'].mean()
    print(f"\nСредняя accuracy модели: {avg_acc:.3f}")
    print(f"Средняя accuracy наивного baseline: {avg_naive:.3f}")
    print(f"Разница: {avg_acc - avg_naive:+.3f}")


if __name__ == '__main__':
    main()