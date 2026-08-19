# services/python-analytics/src/services/analysis_service.py
import pandas as pd
import numpy as np
from typing import Dict, List

class AnalysisService:
    def calculate_indicators(self, df: pd.DataFrame) -> pd.DataFrame:
        """Расчет технических индикаторов"""
        if df.empty:
            return df
        
        df = df.copy()
        
        # Скользящие средние
        df['SMA_20'] = df['close'].rolling(window=20).mean()
        df['SMA_50'] = df['close'].rolling(window=50).mean()
        
        # RSI
        delta = df['close'].diff()
        gain = (delta.where(delta > 0, 0)).rolling(window=14).mean()
        loss = (-delta.where(delta < 0, 0)).rolling(window=14).mean()
        rs = gain / loss
        df['RSI'] = 100 - (100 / (1 + rs))
        
        # Bollinger Bands
        df['BB_middle'] = df['close'].rolling(window=20).mean()
        bb_std = df['close'].rolling(window=20).std()
        df['BB_upper'] = df['BB_middle'] + (bb_std * 2)
        df['BB_lower'] = df['BB_middle'] - (bb_std * 2)
        
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