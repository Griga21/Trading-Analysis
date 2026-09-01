# services/python-analytics/src/ui/main_window.py
import sys
import os
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from PyQt5.QtWidgets import (QMainWindow, QWidget, QVBoxLayout, QHBoxLayout,
                             QComboBox, QPushButton, QTabWidget, QLabel,
                             QSpinBox, QTableWidget, QTableWidgetItem,
                             QSplitter, QFrame, QCheckBox)
from PyQt5.QtCore import Qt, QTimer
from PyQt5.QtGui import QFont
import pyqtgraph as pg
from pyqtgraph import DateAxisItem
import numpy as np
from datetime import datetime, timedelta

from services.database_service import DatabaseService
from services.analysis_service import AnalysisService

class MainWindow(QMainWindow):
    def __init__(self):
        super().__init__()
        self.setWindowTitle("📊 Trading Analysis Dashboard")
        self.resize(1400, 900)
        
        # Сервисы
        self.db_service = DatabaseService()
        self.analysis_service = AnalysisService()
        
        # Текущие данные
        self.current_data = None
        self.current_security = None
        
        # Настройка UI
        self.setup_ui()
        
        # Таймер обновления
        self.timer = QTimer()
        self.timer.timeout.connect(self.refresh_data)
        self.timer.start(60000)
        
        # Загрузка данных
        self.load_securities()
        
        # Стили
        self.setStyleSheet("""
            QMainWindow { background-color: #1a1a2e; }
            QLabel { color: #ffffff; font-size: 14px; }
            QComboBox, QSpinBox {
                background-color: #16213e;
                color: #ffffff;
                padding: 5px;
                border: 1px solid #3498db;
                border-radius: 3px;
                min-width: 150px;
            }
            QPushButton {
                background-color: #3498db;
                color: white;
                padding: 8px 15px;
                border: none;
                border-radius: 3px;
                font-weight: bold;
            }
            QPushButton:hover { background-color: #2980b9; }
            QCheckBox {
                color: #ffffff;
                font-size: 14px;
            }
            QCheckBox::indicator {
                width: 18px;
                height: 18px;
            }
            QCheckBox::indicator:checked {
                background-color: #3498db;
                border: 2px solid #2980b9;
                border-radius: 3px;
            }
            QTabWidget::pane {
                border: 1px solid #3498db;
                background-color: #16213e;
            }
            QTabBar::tab {
                background-color: #1a1a2e;
                color: #ffffff;
                padding: 8px 15px;
                margin-right: 2px;
            }
            QTabBar::tab:selected { background-color: #3498db; }
        """)
    
    def setup_ui(self):
        """Настройка интерфейса"""
        central_widget = QWidget()
        self.setCentralWidget(central_widget)
        main_layout = QVBoxLayout(central_widget)
        
        # Верхняя панель
        top_panel = self.create_top_panel()
        main_layout.addLayout(top_panel)
        
        # Панель выбора линий
        lines_panel = self.create_lines_panel()
        main_layout.addLayout(lines_panel)
        
        # Информационная панель
        info_panel = self.create_info_panel()
        main_layout.addLayout(info_panel)
        
        # Вкладки
        self.tab_widget = QTabWidget()
        self.create_price_tab()
        self.create_indicators_tab()
        self.create_table_tab()
        main_layout.addWidget(self.tab_widget)
    
    def create_top_panel(self):
        """Создание верхней панели"""
        panel = QHBoxLayout()
        
        panel.addWidget(QLabel("Инструмент:"))
        self.security_combo = QComboBox()
        self.security_combo.currentIndexChanged.connect(self.on_security_changed)
        panel.addWidget(self.security_combo)
        
        panel.addWidget(QLabel("Период (дней):"))
        self.period_spin = QSpinBox()
        self.period_spin.setRange(7, 900)
        self.period_spin.setValue(30)
        self.period_spin.valueChanged.connect(self.on_period_changed)
        panel.addWidget(self.period_spin)
        
        self.refresh_btn = QPushButton("🔄 Обновить")
        self.refresh_btn.clicked.connect(self.refresh_data)
        panel.addWidget(self.refresh_btn)
        
        panel.addStretch()
        return panel
    
    def create_lines_panel(self):
        """Создание панели выбора линий"""
        panel = QHBoxLayout()
        panel.addWidget(QLabel("Отображать:"))
        
        # Чекбоксы для выбора линий
        self.cb_price = QCheckBox("Цена")
        self.cb_price.setChecked(True)
        self.cb_price.stateChanged.connect(self.on_lines_changed)
        panel.addWidget(self.cb_price)
        
        self.cb_sma20 = QCheckBox("SMA 200")
        self.cb_sma20.setChecked(True)
        self.cb_sma20.stateChanged.connect(self.on_lines_changed)
        panel.addWidget(self.cb_sma20)
        
        self.cb_sma50 = QCheckBox("SMA 50")
        self.cb_sma50.setChecked(True)
        self.cb_sma50.stateChanged.connect(self.on_lines_changed)
        panel.addWidget(self.cb_sma50)
        
        self.cb_ema20 = QCheckBox("EMA 20")
        self.cb_ema20.setChecked(True)
        self.cb_ema20.stateChanged.connect(self.on_lines_changed)
        panel.addWidget(self.cb_ema20)
        
        self.cb_golden_cross = QCheckBox("Golden cross")
        self.cb_golden_cross.setChecked(True)
        self.cb_golden_cross.stateChanged.connect(self.on_lines_changed)
        panel.addWidget(self.cb_golden_cross)
        
        self.cb_death_cross = QCheckBox("Death cross")
        self.cb_death_cross.setChecked(True)
        self.cb_death_cross.stateChanged.connect(self.on_lines_changed)
        panel.addWidget(self.cb_death_cross)
        
        
        self.cb_avg = QCheckBox("Средняя цена")
        self.cb_avg.setChecked(False)
        self.cb_avg.stateChanged.connect(self.on_lines_changed)
        panel.addWidget(self.cb_avg)
        
        self.cb_bollinger = QCheckBox("Bollinger Bands")
        self.cb_bollinger.setChecked(False)
        self.cb_bollinger.stateChanged.connect(self.on_lines_changed)
        panel.addWidget(self.cb_bollinger)
        
        panel.addStretch()
        return panel
    
    def on_lines_changed(self):
        """Обработка изменения чекбоксов"""
        if self.current_data is not None:
            self.update_price_plot(self.current_data)
    
    def create_info_panel(self):
        """Создание информационной панели"""
        panel = QHBoxLayout()
        
        self.price_label = QLabel("Цена: -")
        self.price_label.setFont(QFont("Arial", 16, QFont.Bold))
        panel.addWidget(self.price_label)
        
        self.change_label = QLabel("Изменение: -")
        panel.addWidget(self.change_label)
        
        self.volume_label = QLabel("Объем: -")
        panel.addWidget(self.volume_label)
        
        panel.addStretch()
        return panel
    
    def create_price_tab(self):
        """Создание вкладки с графиком цены"""
        # Один график для всего
        date_axis = DateAxisItem(orientation='bottom')
        self.price_plot = pg.PlotWidget(axisItems={'bottom': date_axis})
        self.price_plot.setBackground('#16213e')
        self.price_plot.showGrid(x=True, y=True, alpha=0.3)
        self.price_plot.setLabel('left', 'Цена', units='₽')
        self.price_plot.setLabel('bottom', 'Дата')
        self.price_plot.addLegend()
        
        self.tab_widget.addTab(self.price_plot, "💰 График")
    
    def create_indicators_tab(self):
        """Создание вкладки с индикаторами"""
        rsi_axis = DateAxisItem(orientation='bottom')
        self.rsi_plot = pg.PlotWidget(axisItems={'bottom': rsi_axis})
        self.rsi_plot.setBackground('#16213e')
        self.rsi_plot.showGrid(x=True, y=True, alpha=0.3)
        self.rsi_plot.setLabel('left', 'RSI')
        self.rsi_plot.setLabel('bottom', 'Дата')
        self.rsi_plot.setYRange(0, 100)
        
        self.rsi_plot.addLine(y=70, pen=pg.mkPen('r', width=1, style=Qt.PenStyle.DashLine))
        self.rsi_plot.addLine(y=30, pen=pg.mkPen('g', width=1, style=Qt.PenStyle.DashLine))
        
        self.tab_widget.addTab(self.rsi_plot, "📈 RSI")
    
    def create_table_tab(self):
        """Создание вкладки с таблицей"""
        table_widget = QWidget()
        layout = QVBoxLayout(table_widget)
        
        self.table = QTableWidget()
        self.table.setColumnCount(6)
        self.table.setHorizontalHeaderLabels(['Дата', 'Open', 'High', 'Low', 'Close', 'Volume'])
        self.table.setStyleSheet("""
            QTableWidget {
                background-color: #16213e;
                color: #ffffff;
                gridline-color: #3498db;
            }
            QHeaderView::section {
                background-color: #1a1a2e;
                color: #ffffff;
                padding: 5px;
                border: 1px solid #3498db;
            }
        """)
        layout.addWidget(self.table)
        
        self.tab_widget.addTab(table_widget, "📋 Данные")
    
    def load_securities(self):
        """Загрузка списка инструментов"""
        try:
            securities = self.db_service.get_securities()
            self.security_combo.clear()
            self.security_combo.addItems(securities)
        except Exception as e:
            print(f"Ошибка загрузки инструментов: {e}")
            self.security_combo.addItem("Нет данных")
    
    def on_security_changed(self, index):
        if index >= 0:
            self.current_security = self.security_combo.currentText()
            self.refresh_data()
    
    def on_period_changed(self, value):
        self.refresh_data()
    
    def refresh_data(self):
        """Обновление данных"""
        if not self.current_security:
            return
        
        days = self.period_spin.value()
        end_date = datetime.now()
        start_date = end_date - timedelta(days=days)
        
        try:
            df = self.db_service.get_candles(
                self.current_security,
                start_date.strftime('%Y-%m-%d'),
                end_date.strftime('%Y-%m-%d')
            )
            
            if df.empty:
                return
            
            df = self.analysis_service.calculate_indicators(df)
            df = self.analysis_service.detect_ma_crossover(df, fast_col='SMA_200', slow_col='SMA_50')
            
            self.current_data = df
            
            self.update_price_plot(df)
            self.update_rsi_plot(df)
            self.update_table(df)
            self.update_info_panel(df)
            
        except Exception as e:
            print(f"Ошибка обновления данных: {e}")
    
    def update_price_plot(self, df):
        """Обновление графика с учетом выбранных линий"""
        self.price_plot.clear()
        
        if df.empty:
            return
        
        dates = df.index.astype(np.int64) // 10**9
        
        # Свечной график (всегда показываем)
        for i in range(len(df)):
            color = 'g' if df['close'].iloc[i] >= df['open'].iloc[i] else 'r'
            self.price_plot.plot([dates[i], dates[i]], 
                                [df['low'].iloc[i], df['high'].iloc[i]], 
                                pen=pg.mkPen(color, width=1))
            self.price_plot.plot([dates[i], dates[i]], 
                                [df['open'].iloc[i], df['close'].iloc[i]], 
                                pen=pg.mkPen(color, width=4))
        
        # Линия цены закрытия
        if self.cb_price.isChecked():
            self.price_plot.plot(dates, df['close'].values, 
                                pen=pg.mkPen('#ffffff', width=1),
                                name='Цена закрытия')
        
        # Средняя цена
        if self.cb_avg.isChecked():
            avg_price = (df['high'] + df['low']) / 2
            self.price_plot.plot(dates, avg_price.values, 
                                pen=pg.mkPen('#00ff88', width=2),
                                name='Средняя цена')
        
        # SMA 200
        if self.cb_sma20.isChecked() and 'SMA_200' in df.columns:
            valid = df.dropna(subset=['SMA_200'])
            if not valid.empty:
                self.price_plot.plot(valid.index.astype(np.int64) // 10**9,
                                    valid['SMA_200'].values,
                                    pen=pg.mkPen('y', width=2),
                                    name='SMA 200')
        
        # SMA 50
        if self.cb_sma50.isChecked() and 'SMA_50' in df.columns:
            valid = df.dropna(subset=['SMA_50'])
            if not valid.empty:
                self.price_plot.plot(valid.index.astype(np.int64) // 10**9,
                                    valid['SMA_50'].values,
                                    pen=pg.mkPen('b', width=2),
                                    name='SMA 50')
        
        # EMA 20
        if self.cb_ema20.isChecked() and 'EMA_20' in df.columns:
            valid = df.dropna(subset=['EMA_20'])
            if not valid.empty:
                self.price_plot.plot(valid.index.astype(np.int64) // 10**9,
                                    valid['EMA_20'].values,
                                    pen=pg.mkPen('r', width=2),
                                    name='EMA 20')
                
    
        if self.cb_golden_cross.isChecked() and 'golden_cross' in df.columns:
            golden_points = df[df['golden_cross'] == True]
            if not golden_points.empty:
                golden_scatter = pg.ScatterPlotItem(
                    x=golden_points.index.astype(np.int64) // 10**9,
                    y=golden_points['low'].values * 0.97,  # сдвиг ниже свечи
                    symbol='t',       # треугольник вверх
                    size=20,          # увеличили размер
                    brush=pg.mkBrush('lime'),
                    pen=pg.mkPen('white', width=1.5),  # контрастная обводка
                    name='Golden Cross'
                )
                self.price_plot.addItem(golden_scatter)
                golden_scatter.setZValue(10)  # поверх свечей

        if self.cb_death_cross.isChecked() and 'death_cross' in df.columns:
            death_points = df[df['death_cross'] == True]
            if not death_points.empty:
                death_scatter = pg.ScatterPlotItem(
                    x=death_points.index.astype(np.int64) // 10**9,
                    y=death_points['high'].values * 1.03,  # сдвиг выше свечи
                    symbol='t1',      # треугольник вниз
                    size=20,
                    brush=pg.mkBrush('red'),
                    pen=pg.mkPen('white', width=1.5),
                    name='Death Cross'
                )
                self.price_plot.addItem(death_scatter)
                death_scatter.setZValue(10)
        
        # Bollinger Bands
        if self.cb_bollinger.isChecked() and 'BB_upper' in df.columns:
            valid = df.dropna(subset=['BB_upper'])
            if not valid.empty:
                valid_dates = valid.index.astype(np.int64) // 10**9
                self.price_plot.plot(valid_dates, valid['BB_upper'].values,
                                    pen=pg.mkPen('#ff8800', width=1, style=Qt.PenStyle.DashLine),
                                    name='BB Upper')
                self.price_plot.plot(valid_dates, valid['BB_lower'].values,
                                    pen=pg.mkPen('#ff8800', width=1, style=Qt.PenStyle.DashLine),
                                    name='BB Lower')
        
        self.price_plot.autoRange()
    
    def update_rsi_plot(self, df):
        """Обновление графика RSI"""
        self.rsi_plot.clear()
        
        if df.empty or 'RSI' not in df.columns:
            return
        
        valid_data = df.dropna(subset=['RSI'])
        if not valid_data.empty:
            dates = valid_data.index.astype(np.int64) // 10**9
            self.rsi_plot.plot(dates, valid_data['RSI'].values, 
                              pen=pg.mkPen('y', width=2))
            
            self.rsi_plot.addLine(y=70, pen=pg.mkPen('r', width=1, style=Qt.PenStyle.DashLine))
            self.rsi_plot.addLine(y=30, pen=pg.mkPen('g', width=1, style=Qt.PenStyle.DashLine))
            
            self.rsi_plot.autoRange()
    
    def update_table(self, df):
        """Обновление таблицы"""
        if df.empty:
            return
        
        recent_df = df.tail(100)
        
        self.table.setRowCount(len(recent_df))
        for i, (index, row) in enumerate(recent_df.iterrows()):
            self.table.setItem(i, 0, QTableWidgetItem(str(index)))
            self.table.setItem(i, 1, QTableWidgetItem(f"{row['open']:.2f}"))
            self.table.setItem(i, 2, QTableWidgetItem(f"{row['high']:.2f}"))
            self.table.setItem(i, 3, QTableWidgetItem(f"{row['low']:.2f}"))
            self.table.setItem(i, 4, QTableWidgetItem(f"{row['close']:.2f}"))
            self.table.setItem(i, 5, QTableWidgetItem(f"{row['volume']:.0f}"))
    
    def update_info_panel(self, df):
        """Обновление информационной панели"""
        if df.empty:
            return
        
        try:
            summary = self.analysis_service.get_summary(df)
            
            self.price_label.setText(f"Цена: {summary['last_price']:.2f} ₽")
            self.change_label.setText(
                f"Изменение: {summary['change']:+.2f} ₽ ({summary['change_percent']:+.2f}%)"
            )
            self.volume_label.setText(f"Объем: {summary['avg_volume']:.0f}")
        except Exception as e:
            print(f"Ошибка обновления информации: {e}")