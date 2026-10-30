# services/python-analytics/src/services/analysis_service.py
import pandas as pd
import numpy as np
from typing import Dict, List

class AnalysisService:
    def calculate_indicators(self, df: pd.DataFrame) -> pd.DataFrame:
        """Calculate the indicators shared by the UI and the model."""
        if df.empty:
            return df
        
        df = df.copy()
        
        # Moving averages
        df['SMA_20'] = df['close'].rolling(window=20, min_periods=20).mean()
        df['SMA_50'] = df['close'].rolling(window=50, min_periods=50).mean()
        df['SMA_200'] = df['close'].rolling(window=200).mean()
        df['EMA_20'] = df['close'].ewm(span=20, adjust=False).mean()
        
        # RSI
        delta = df['close'].diff()
        gain = delta.clip(lower=0).rolling(window=14, min_periods=14).mean()
        loss = -delta.clip(upper=0).rolling(window=14, min_periods=14).mean()
        rs = gain.div(loss.replace(0, np.nan))
        rsi = 100 - (100 / (1 + rs))
        rsi = rsi.mask((loss == 0) & (gain > 0), 100)
        rsi = rsi.mask((gain == 0) & (loss > 0), 0)
        df['RSI'] = rsi.mask((gain == 0) & (loss == 0), 50)
        
        # Bollinger Bands
        df['BB_middle'] = df['close'].rolling(window=20, min_periods=20).mean()
        bb_std = df['close'].rolling(window=20).std()
        df['BB_upper'] = df['BB_middle'] + (bb_std * 2)
        df['BB_lower'] = df['BB_middle'] - (bb_std * 2)

        # MACD
        ema_12 = df['close'].ewm(span=12, adjust=False).mean()
        ema_26 = df['close'].ewm(span=26, adjust=False).mean()
        df['MACD'] = ema_12 - ema_26
        df['MACD_signal'] = df['MACD'].ewm(span=9, adjust=False).mean()
        df['MACD_histogram'] = df['MACD'] - df['MACD_signal']

        # Returns and rolling volatility
        df['returns'] = df['close'].pct_change()
        df['volatility_20'] = df['returns'].rolling(window=20, min_periods=20).std()
        
        return df
    
    def get_summary(self, df: pd.DataFrame) -> Dict:
        """Получение сводки по данным"""
        if df.empty:
            return {}
        
        last_close = df['close'].iloc[-1]
        prev_close = df['close'].iloc[-2] if len(df) > 1 else last_close
        change = last_close - prev_close
        change_percent = (change / prev_close * 100) if prev_close else 0
        
        return {
            'last_price': last_close,
            'change': change,
            'change_percent': change_percent,
            'avg_volume': df['volume'].mean(),
            'high_52w': df['high'].tail(252).max() if len(df) >= 252 else df['high'].max(),
            'low_52w': df['low'].tail(252).min() if len(df) >= 252 else df['low'].min(),
        }
        
    def detect_ma_crossover(self, df: pd.DataFrame, fast_col='SMA_20', slow_col='SMA_50') -> pd.DataFrame:
        df = df.copy()
        df['ma_diff'] = df[fast_col] - df[slow_col]
        df['ma_diff_prev'] = df['ma_diff'].shift(1)
        
        df['golden_cross'] = (df['ma_diff'] > 0) & (df['ma_diff_prev'] <= 0)
        df['death_cross'] = (df['ma_diff'] < 0) & (df['ma_diff_prev'] >= 0)
        
        return df
    
    def calculate_macd(self, df: pd.DataFrame) -> pd.DataFrame:
        df = self.calculate_indicators(df)

        df['macd_bullish_cross'] = (df['MACD'] > df['MACD_signal']) & (df['MACD'].shift(1) <= df['MACD_signal'].shift(1))
        df['macd_bearish_cross'] = (df['MACD'] < df['MACD_signal']) & (df['MACD'].shift(1) >= df['MACD_signal'].shift(1))
        
        return df