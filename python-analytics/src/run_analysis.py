import argparse
import json
import sys
from datetime import datetime
from typing import List

from services.database_service import DatabaseService
from services.notification_service import NotificationDeliveryError, send_signal_notification
from training.trainer import train_and_evaluate


def analyze_ticker(
    database: DatabaseService,
    ticker: str,
    as_of: str,
    horizon: int,
    threshold: float,
) -> dict:
    candles = database.get_candles(ticker, end_date=as_of)
    if candles.empty:
        raise ValueError(f"No candle data available for {ticker}")

    result = train_and_evaluate(
        candles,
        ticker=ticker,
        horizon=horizon,
        probability_threshold=threshold,
    )
    return {
        "signal": result["signal"],
        "results_by_fold": result["results_by_fold"],
        "feature_importance": result["feature_importance"],
    }


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Evaluate the XGBoost model and return a research signal for closed daily candles."
    )
    parser.add_argument("ticker", nargs="?", help="Ticker to analyze, for example SBER")
    parser.add_argument(
        "--all-active",
        action="store_true",
        help="Analyze all active securities configured in the database",
    )
    parser.add_argument(
        "--as-of",
        required=True,
        help="Date of the latest completed daily candle (YYYY-MM-DD)",
    )
    parser.add_argument("--horizon", type=int, default=15, help="Forecast horizon in trading candles")
    parser.add_argument(
        "--threshold",
        type=float,
        default=0.60,
        help="Minimum probability for a BUY_CANDIDATE signal",
    )
    parser.add_argument(
        "--no-notify",
        action="store_true",
        help="Print signals without sending Telegram notifications",
    )
    args = parser.parse_args()

    try:
        datetime.strptime(args.as_of, "%Y-%m-%d")
    except ValueError:
        parser.error("--as-of must be a date in YYYY-MM-DD format")
    if args.horizon < 1:
        parser.error("--horizon must be a positive number")
    if not 0.5 < args.threshold <= 1:
        parser.error("--threshold must be greater than 0.5 and at most 1")
    if args.all_active == bool(args.ticker):
        parser.error("Provide one ticker or use --all-active")

    database = DatabaseService()
    tickers: List[str] = database.get_securities() if args.all_active else [args.ticker]
    if not tickers:
        print("No active securities found in the database.", file=sys.stderr)
        return 1

    results = []
    exit_code = 0
    for ticker in tickers:
        try:
            results.append(
                analyze_ticker(
                    database,
                    ticker,
                    args.as_of,
                    args.horizon,
                    args.threshold,
                )
            )
        except ValueError as error:
            exit_code = 1
            print(f"{ticker}: {error}", file=sys.stderr)

    output = results[0] if not args.all_active and results else results
    if output:
        print(json.dumps(output, ensure_ascii=False, indent=2))

    if not args.no_notify:
        for result in results:
            signal = result["signal"]
            if signal["status"] != "BUY_CANDIDATE":
                continue
            try:
                send_signal_notification(signal)
                print(f"Telegram notification sent for {signal['ticker']}.", file=sys.stderr)
            except NotificationDeliveryError as error:
                exit_code = 1
                print(
                    f"Failed to send Telegram notification for {signal['ticker']}: {error}",
                    file=sys.stderr,
                )

    return exit_code


if __name__ == "__main__":
    raise SystemExit(main())
