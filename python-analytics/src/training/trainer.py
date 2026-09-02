import pandas as pd
import numpy as np
import xgboost as xgb
from sklearn.metrics import accuracy_score, precision_score, recall_score

def train_and_evaluate(df: pd.DataFrame, horizon: int = 15) -> dict:
    features = build_feature_matrix(df)
    target = create_target(df, horizon=horizon)

    data = features.join(target.rename('target')).dropna()
    X = data.drop(columns=['close', 'target'])
    y = data['target']

    splits = walk_forward_splits(data, n_splits=5, test_size=60)
    fold_results = []
    model = None

    for fold, (train_idx, test_idx) in enumerate(splits):
        X_train, y_train = X.iloc[train_idx], y.iloc[train_idx]
        X_test, y_test = X.iloc[test_idx], y.iloc[test_idx]

        model = xgb.XGBClassifier(
            n_estimators=100, max_depth=3, learning_rate=0.05,
            eval_metric='logloss'
        )
        model.fit(X_train, y_train)
        preds = model.predict(X_test)

        naive_preds = np.ones_like(y_test)

        fold_results.append({
            'fold': fold,
            'accuracy': accuracy_score(y_test, preds),
            'precision': precision_score(y_test, preds, zero_division=0),
            'recall': recall_score(y_test, preds, zero_division=0),
            'naive_accuracy': accuracy_score(y_test, naive_preds),
            'test_size': len(y_test),
        })

    if model is None:
        raise ValueError("Не удалось обучить модель: splits пуст или данные отсутствуют.")

    importance_df = pd.DataFrame({
        'feature': X.columns,
        'importance': model.feature_importances_
    }).sort_values('importance', ascending=False)

    return {
        'results_by_fold': fold_results,
        'feature_importance': importance_df.to_dict('records'),
        'model': model,
    }

def build_feature_matrix(df: pd.DataFrame) -> pd.DataFrame:
    if df.empty:
        return df
    
    df = df.copy()
    
    feature_columns =[
        'SMA_20', 'SMA_50', 'EMA_20',
        'RSI', 'MACD', 'MACD_signal', 'MACD_histogram',
        'BB_upper', 'BB_lower', 'BB_middle'
    ]
    df['price_to_sma20'] = df['close'] / df['SMA_20'] - 1
    df['price_to_sma50'] = df['close'] / df['SMA_50'] - 1
    df['sma20_to_sma50'] = df['SMA_20'] / df['SMA_50'] - 1
    df['bb_position'] = (df['close'] - df['BB_lower']) / (df['BB_upper'] - df['BB_lower'])

    engineered_cols = ['price_to_sma20', 'price_to_sma50', 'sma20_to_sma50', 'bb_position']

    all_features = feature_columns + engineered_cols
    return df[all_features + ['close']].copy()

def create_target(df: pd.DataFrame, horizon: int = 15, threshold: float = 0.0) -> pd.Series:
    future_return = df['close'].shift(-horizon) / df['close'] - 1
    target = (future_return > threshold).astype(int)
    return target

def walk_forward_splits(df: pd.DataFrame, n_splits: int = 5, test_size: int = 60):
    n = len(df)
    splits = []
    for i in range(n_splits):
        test_end = n - (n_splits - i - 1) * test_size
        test_start = test_end - test_size
        train_end = test_start

        if train_end < 100:  # минимум данных для обучения
            continue

        train_idx = list(range(0, train_end))
        test_idx = list(range(test_start, test_end))
        splits.append((train_idx, test_idx))

    return splits