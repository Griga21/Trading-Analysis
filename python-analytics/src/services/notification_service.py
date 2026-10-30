import json
import os
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen


class NotificationDeliveryError(RuntimeError):
    pass


def send_signal_notification(signal: dict) -> None:
    endpoint = os.getenv("ANALYSIS_NOTIFY_URL")
    token = os.getenv("ANALYSIS_NOTIFY_TOKEN")
    if not endpoint or not token:
        raise NotificationDeliveryError(
            "Set ANALYSIS_NOTIFY_URL and ANALYSIS_NOTIFY_TOKEN to enable Telegram delivery"
        )

    body = json.dumps(
        {
            "ticker": signal["ticker"],
            "asOf": signal["as_of"],
            "close": signal["close"],
            "horizon": signal["horizon"],
            "probabilityUp": signal["probability_up"],
            "threshold": signal["threshold"],
        },
        ensure_ascii=False,
    ).encode("utf-8")
    request = Request(
        endpoint,
        data=body,
        headers={
            "Content-Type": "application/json",
            "X-Analytics-Token": token,
        },
        method="POST",
    )

    try:
        with urlopen(request, timeout=10) as response:
            if not 200 <= response.status < 300:
                raise NotificationDeliveryError(
                    f"Telegram service returned HTTP {response.status}"
                )
    except HTTPError as error:
        raise NotificationDeliveryError(
            f"Telegram service returned HTTP {error.code}"
        ) from error
    except URLError as error:
        raise NotificationDeliveryError(
            f"Could not reach Telegram service: {error.reason}"
        ) from error
