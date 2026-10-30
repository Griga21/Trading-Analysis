import numpy as np
import pandas as pd
import pytest

from training import trainer


def make_candles(count=700):
    steps = np.arange(count)
    close = 100 + steps * 0.04 + np.sin(steps / 10) * 4
    return pd.DataFrame(
        {
            "open": close,
            "high": close + 1,
            "low": close - 1,
            "close": close,
            "volume": 1000 + (steps % 25) * 10,
        },
        index=pd.date_range("2020-01-01", periods=count, freq="B"),
    )


def test_target_leaves_unknown_future_returns_unlabeled():
    candles = pd.DataFrame({"close": [100.0, 102.0, 101.0, 90.0, 109.0]})

    target = trainer.create_target(candles, horizon=2)

    assert target.iloc[:3].tolist() == [1, 0, 1]
    assert target.iloc[3:].isna().all()


def test_walk_forward_purges_horizon_using_original_positions():
    source_positions = np.array([0, 1, 2, 3, 4, 5, 8, 9, 10, 11, 12, 13])
    splits = trainer.walk_forward_splits(
        pd.DataFrame(index=range(len(source_positions))),
        n_splits=2,
        test_size=3,
        horizon=2,
        min_train_size=4,
        source_positions=source_positions,
    )

    assert len(splits) == 2
    for train_indices, test_indices in splits:
        latest_training_position = source_positions[train_indices[-1]]
        first_test_position = source_positions[test_indices[0]]
        assert latest_training_position + 2 < first_test_position


@pytest.mark.parametrize(
    ("probability", "expected_status"),
    [(0.60, "BUY_CANDIDATE"), (0.599, "NO_SIGNAL")],
)
def test_signal_threshold_boundary(monkeypatch, probability, expected_status):
    monkeypatch.setattr(
        trainer,
        "_positive_probability",
        lambda model, features: np.array([probability]),
    )
    candle = pd.Series(
        {"close": 100.0},
        name=pd.Timestamp("2025-01-02"),
    )

    signal = trainer._build_signal(
        model=None,
        latest_features=pd.DataFrame({"feature": [0.0]}),
        latest_candle=candle,
        ticker="TEST",
        horizon=15,
        probability_threshold=0.60,
    )

    assert signal.status == expected_status
    assert signal.as_of == "2025-01-02T00:00:00"


def test_train_and_evaluate_returns_signal_for_last_candle():
    candles = make_candles()

    result = trainer.train_and_evaluate(
        candles,
        ticker="TEST",
        n_splits=2,
        test_size=30,
        min_train_size=100,
    )

    assert result["signal"]["ticker"] == "TEST"
    assert result["signal"]["as_of"] == candles.index[-1].isoformat()
    assert result["signal"]["status"] in {"BUY_CANDIDATE", "NO_SIGNAL"}
    assert len(result["results_by_fold"]) == 2
    assert result["feature_importance"]


def test_train_and_evaluate_rejects_non_timestamp_index():
    candles = make_candles()
    candles.index = range(len(candles))

    with pytest.raises(ValueError, match="timestamps"):
        trainer.train_and_evaluate(candles)
