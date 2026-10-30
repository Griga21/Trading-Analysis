import numpy as np
import pandas as pd

from services.analysis_service import AnalysisService


def test_flat_prices_have_neutral_rsi():
    candles = pd.DataFrame(
        {"close": np.full(30, 100.0), "volume": np.full(30, 1000.0)}
    )

    result = AnalysisService().calculate_indicators(candles)

    assert result["RSI"].iloc[-1] == 50
