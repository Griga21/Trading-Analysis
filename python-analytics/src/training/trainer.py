from dataclasses import asdict, dataclass
from typing import List, Optional, Tuple

import numpy as np
import pandas as pd
import xgboost as xgb
from sklearn.metrics import (
    accuracy_score,
    balanced_accuracy_score,
    brier_score_loss,
    precision_score,
    recall_score,
)

from services.analysis_service import AnalysisService


DEFAULT_HORIZON = 15
DEFAULT_SIGNAL_THRESHOLD = 0.60
DEFAULT_N_SPLITS = 5
DEFAULT_TEST_SIZE = 60
MIN_TRAIN_SIZE = 100

FEATURE_COLUMNS = [
    "close_to_sma20",
    "close_to_sma50",
    "close_to_sma200",
    "close_to_ema20",
    "sma20_to_sma50",
    "sma50_to_sma200",
    "rsi",
    "macd_to_close",
    "macd_signal_to_close",
    "macd_histogram_to_close",
    "bb_position",
    "volatility_20",
    "return_1",
    "return_5",
    "volume_ratio_20",
]


@dataclass(frozen=True)
class SignalPrediction:
    ticker: str
    as_of: str
    close: float
    horizon: int
    probability_up: float
    threshold: float
    status: str
    message: str

    def to_dict(self) -> dict:
        return asdict(self)


def build_feature_matrix(df: pd.DataFrame) -> pd.DataFrame:
    if df.empty:
        return pd.DataFrame(index=df.index, columns=FEATURE_COLUMNS, dtype=float)

    indicators = AnalysisService().calculate_indicators(df)
    close = indicators["close"].replace(0, np.nan)

    features = pd.DataFrame(index=indicators.index)
    features["close_to_sma20"] = close / indicators["SMA_20"] - 1
    features["close_to_sma50"] = close / indicators["SMA_50"] - 1
    features["close_to_sma200"] = close / indicators["SMA_200"] - 1
    features["close_to_ema20"] = close / indicators["EMA_20"] - 1
    features["sma20_to_sma50"] = indicators["SMA_20"] / indicators["SMA_50"] - 1
    features["sma50_to_sma200"] = indicators["SMA_50"] / indicators["SMA_200"] - 1
    features["rsi"] = indicators["RSI"] / 100
    features["macd_to_close"] = indicators["MACD"] / close
    features["macd_signal_to_close"] = indicators["MACD_signal"] / close
    features["macd_histogram_to_close"] = indicators["MACD_histogram"] / close

    band_width = indicators["BB_upper"] - indicators["BB_lower"]
    features["bb_position"] = (
        (indicators["close"] - indicators["BB_lower"]) / band_width.replace(0, np.nan)
    )
    features["volatility_20"] = indicators["volatility_20"]
    features["return_1"] = indicators["close"].pct_change()
    features["return_5"] = indicators["close"].pct_change(periods=5)

    average_volume = indicators["volume"].rolling(window=20, min_periods=20).mean()
    features["volume_ratio_20"] = indicators["volume"] / average_volume.replace(0, np.nan)

    return features[FEATURE_COLUMNS].replace([np.inf, -np.inf], np.nan)


def create_target(
    df: pd.DataFrame,
    horizon: int = DEFAULT_HORIZON,
    threshold: float = 0.0,
) -> pd.Series:
    if horizon < 1:
        raise ValueError("horizon must be a positive number of candles")
    if not np.isfinite(threshold):
        raise ValueError("threshold must be finite")

    future_return = df["close"].shift(-horizon) / df["close"] - 1
    target = pd.Series(np.nan, index=df.index, name="target", dtype=float)
    known_target = future_return.notna()
    target.loc[known_target] = (future_return.loc[known_target] > threshold).astype(int)
    return target


def walk_forward_splits(
    df: pd.DataFrame,
    n_splits: int = DEFAULT_N_SPLITS,
    test_size: int = DEFAULT_TEST_SIZE,
    horizon: int = DEFAULT_HORIZON,
    min_train_size: int = MIN_TRAIN_SIZE,
    source_positions: Optional[np.ndarray] = None,
) -> List[Tuple[List[int], List[int]]]:
    if n_splits < 1 or test_size < 1 or horizon < 1 or min_train_size < 1:
        raise ValueError("n_splits, test_size, horizon and min_train_size must be positive")

    sample_count = len(df)
    if source_positions is None:
        source_positions = np.arange(sample_count)
    elif len(source_positions) != sample_count or np.any(np.diff(source_positions) <= 0):
        raise ValueError("source_positions must be strictly increasing and match the data length")

    splits = []
    first_test_start = sample_count - n_splits * test_size

    for fold in range(n_splits):
        test_start = first_test_start + fold * test_size
        test_end = test_start + test_size
        if test_start < 0:
            continue

        first_test_source_position = source_positions[test_start]
        train_indices = np.flatnonzero(
            source_positions < first_test_source_position - horizon
        ).tolist()
        if len(train_indices) < min_train_size:
            continue

        splits.append((train_indices, list(range(test_start, test_end))))

    return splits


def _create_model() -> xgb.XGBClassifier:
    return xgb.XGBClassifier(
        n_estimators=200,
        max_depth=3,
        learning_rate=0.03,
        subsample=0.9,
        colsample_bytree=0.9,
        objective="binary:logistic",
        eval_metric="logloss",
        random_state=42,
        n_jobs=1,
    )


def _positive_probability(model: xgb.XGBClassifier, features: pd.DataFrame) -> np.ndarray:
    positive_class_index = np.flatnonzero(model.classes_ == 1)
    if len(positive_class_index) != 1:
        raise ValueError("Training data must contain both target classes")
    return model.predict_proba(features)[:, positive_class_index[0]]


def _build_signal(
    model: xgb.XGBClassifier,
    latest_features: pd.DataFrame,
    latest_candle: pd.Series,
    ticker: str,
    horizon: int,
    probability_threshold: float,
) -> SignalPrediction:
    probability_up = float(_positive_probability(model, latest_features)[0])
    is_candidate = probability_up >= probability_threshold
    as_of = pd.Timestamp(latest_candle.name).isoformat()

    if is_candidate:
        status = "BUY_CANDIDATE"
        message = (
            f"{ticker}: model estimates {probability_up:.1%} probability of a positive "
            f"close-to-close return over the next {horizon} trading candles. "
            "Research signal only; not a trading recommendation."
        )
    else:
        status = "NO_SIGNAL"
        message = (
            f"{ticker}: probability of a positive return is {probability_up:.1%}, "
            f"below the {probability_threshold:.0%} signal threshold."
        )

    return SignalPrediction(
        ticker=ticker,
        as_of=as_of,
        close=float(latest_candle["close"]),
        horizon=horizon,
        probability_up=probability_up,
        threshold=probability_threshold,
        status=status,
        message=message,
    )


def train_and_evaluate(
    df: pd.DataFrame,
    ticker: str = "UNKNOWN",
    horizon: int = DEFAULT_HORIZON,
    probability_threshold: float = DEFAULT_SIGNAL_THRESHOLD,
    n_splits: int = DEFAULT_N_SPLITS,
    test_size: int = DEFAULT_TEST_SIZE,
    min_train_size: int = MIN_TRAIN_SIZE,
) -> dict:
    if not 0.5 < probability_threshold <= 1:
        raise ValueError("probability_threshold must be greater than 0.5 and at most 1")
    if horizon < 1:
        raise ValueError("horizon must be a positive number of candles")
    if df.empty:
        raise ValueError("No candle data available")
    if not isinstance(df.index, pd.DatetimeIndex):
        raise ValueError("Candle data must use timestamps as its index")
    if not df.index.is_monotonic_increasing or df.index.has_duplicates:
        raise ValueError("Candle data must be ordered by timestamp and contain no duplicates")
    if "close" not in df.columns or "volume" not in df.columns:
        raise ValueError("Candle data must contain close and volume columns")
    if not np.isfinite(df[["close", "volume"]].to_numpy(dtype=float)).all():
        raise ValueError("Candle close and volume values must be finite")
    if (df["close"] <= 0).any() or (df["volume"] < 0).any():
        raise ValueError("Candle close must be positive and volume cannot be negative")

    features = build_feature_matrix(df)
    target = create_target(df, horizon=horizon)
    labeled = features.join(target).dropna(subset=FEATURE_COLUMNS + ["target"])
    if labeled.empty:
        raise ValueError("Not enough complete candles to build training features")

    X = labeled[FEATURE_COLUMNS]
    y = labeled["target"].astype(int)
    if y.nunique() != 2:
        raise ValueError("Training history must contain examples from both target classes")

    splits = walk_forward_splits(
        labeled,
        n_splits=n_splits,
        test_size=test_size,
        horizon=horizon,
        min_train_size=min_train_size,
        source_positions=df.index.get_indexer(labeled.index),
    )
    if not splits:
        required_rows = min_train_size + horizon + n_splits * test_size
        raise ValueError(
            f"Not enough labeled candles for walk-forward evaluation; "
            f"need at least {required_rows}, got {len(labeled)}"
        )

    fold_results = []
    for fold, (train_indices, test_indices) in enumerate(splits, start=1):
        X_train, y_train = X.iloc[train_indices], y.iloc[train_indices]
        X_test, y_test = X.iloc[test_indices], y.iloc[test_indices]
        if y_train.nunique() != 2:
            raise ValueError(f"Walk-forward fold {fold} training data contains only one class")

        fold_model = _create_model()
        fold_model.fit(X_train, y_train)
        probabilities = _positive_probability(fold_model, X_test)
        predictions = (probabilities >= 0.5).astype(int)
        majority_class = int(y_train.mean() >= 0.5)
        baseline_predictions = np.full(len(y_test), majority_class, dtype=int)

        fold_results.append({
            "fold": fold,
            "accuracy": float(accuracy_score(y_test, predictions)),
            "balanced_accuracy": float(balanced_accuracy_score(y_test, predictions)),
            "precision": float(precision_score(y_test, predictions, zero_division=0)),
            "recall": float(recall_score(y_test, predictions, zero_division=0)),
            "brier_score": float(brier_score_loss(y_test, probabilities)),
            "baseline_accuracy": float(accuracy_score(y_test, baseline_predictions)),
            "test_size": len(y_test),
        })

    final_model = _create_model()
    final_model.fit(X, y)

    latest_features = features.dropna(subset=FEATURE_COLUMNS).tail(1)
    if latest_features.empty:
        raise ValueError("Latest candle does not have enough history to calculate all features")
    if latest_features.index[-1] != df.index[-1]:
        raise ValueError("Latest candle does not have complete features; refusing a stale signal")
    latest_candle = df.loc[latest_features.index[-1]]
    signal = _build_signal(
        final_model,
        latest_features,
        latest_candle,
        ticker.upper(),
        horizon,
        probability_threshold,
    )

    importance = pd.DataFrame({
        "feature": FEATURE_COLUMNS,
        "importance": final_model.feature_importances_,
    }).sort_values("importance", ascending=False)
    feature_importance = [
        {"feature": row.feature, "importance": float(row.importance)}
        for row in importance.itertuples(index=False)
    ]

    return {
        "ticker": ticker.upper(),
        "horizon": horizon,
        "target_definition": f"close return over next {horizon} candles > 0",
        "signal": signal.to_dict(),
        "results_by_fold": fold_results,
        "feature_importance": feature_importance,
        "model": final_model,
    }
