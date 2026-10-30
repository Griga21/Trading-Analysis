# database_service.py
import os
from typing import List, Optional

import pandas as pd
import psycopg2


class DatabaseService:
    def __init__(
        self,
        host: Optional[str] = None,
        port: Optional[int] = None,
        database: Optional[str] = None,
        user: Optional[str] = None,
        password: Optional[str] = None,
    ):
        self.connection_params = {
            'host': host or os.getenv('DB_HOST', 'localhost'),
            'port': port or int(os.getenv('DB_PORT', '5432')),
            'database': database or os.getenv('DB_NAME', 'trading_data'),
            'user': user or os.getenv('DB_USER', 'postgres'),
            'password': password or os.getenv('DB_PASSWORD', 'root')
        }
    
    def get_connection(self):
        """Создание подключения к базе данных"""
        return psycopg2.connect(**self.connection_params)
    
    def get_candles(self, security_id: str, start_date: Optional[str] = None,
                    end_date: Optional[str] = None) -> pd.DataFrame:
        """Получение свечей для инструмента"""
        query = """
            SELECT 
                timestamp,
                open,
                high,
                low,
                close,
                volume
            FROM candles
            WHERE security_id = %s
        """
        params = [security_id]
        
        if start_date:
            query += " AND timestamp >= %s"
            params.append(start_date)
        if end_date:
            query += " AND timestamp < CAST(%s AS date) + INTERVAL '1 day'"
            params.append(end_date)
        
        query += " ORDER BY timestamp"
        
        with self.get_connection() as conn:
            df = pd.read_sql_query(query, conn, params=tuple(params))
        
        if not df.empty:
            df['timestamp'] = pd.to_datetime(df['timestamp'])
            df.set_index('timestamp', inplace=True)
        
        return df
    
    def get_securities(self) -> List[str]:
        """Get active securities configured for collection."""
        query = "SELECT security_id FROM securities WHERE is_active = TRUE ORDER BY security_id"
        
        with self.get_connection() as conn:
            df = pd.read_sql_query(query, conn)
        
        return df['security_id'].tolist()
    
    def get_last_price(self, security_id: str) -> float:
        """Получение последней цены"""
        query = """
            SELECT close 
            FROM candles 
            WHERE security_id = %s 
            ORDER BY timestamp DESC 
            LIMIT 1
        """
        
        with self.get_connection() as conn:
            df = pd.read_sql_query(query, conn, params=[security_id])
        
        return df['close'].iloc[0] if not df.empty else 0.0
    
    def get_statistics(self, security_id: str) -> dict:
        """Получение статистики по инструменту"""
        query = """
            SELECT 
                COUNT(*) as count,
                AVG(close) as avg_price,
                MIN(close) as min_price,
                MAX(close) as max_price,
                STDDEV(close) as std_dev
            FROM candles
            WHERE security_id = %s
        """
        
        with self.get_connection() as conn:
            df = pd.read_sql_query(query, conn, params=[security_id])
        
        return df.iloc[0].to_dict() if not df.empty else {}