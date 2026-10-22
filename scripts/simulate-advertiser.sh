#!/usr/bin/env bash
#
# Plays the advertiser's server: reports a booking for a click, or its
# cancellation, the way a real partner integration would.
#
# The API key comes from the API_KEY environment variable, not an argument,
# so it does not appear in the process list or in shell history.
#
# Usage:
#   API_KEY=ak_... ./scripts/simulate-advertiser.sh book   <click-token> <booking-id> <value> [currency]
#   API_KEY=ak_... ./scripts/simulate-advertiser.sh cancel <click-token> <booking-id>
#
# Optional:
#   BASE_URL      platform address (default http://localhost:8080)
#   CONVERTED_AT  booking time, ISO-8601 UTC (default: now)

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"

usage() {
    sed -n '2,16p' "$0" | sed 's/^# \{0,1\}//'
    exit 1
}

if [[ -z "${API_KEY:-}" ]]; then
    echo "API_KEY is not set. Issue a key first (POST /api/v1/advertiser-api-keys)." >&2
    exit 1
fi

[[ $# -lt 1 ]] && usage
command="$1"
shift

post() {
    local path="$1"
    local body="$2"
    curl -s -X POST "${BASE_URL}${path}" \
        -H "X-Api-Key: ${API_KEY}" \
        -H "Content-Type: application/json" \
        -w "\nHTTP %{http_code}\n" \
        -d "$body"
}

case "$command" in
    book)
        [[ $# -lt 3 ]] && usage
        click_token="$1"
        booking_id="$2"
        value="$3"
        currency="${4:-USD}"
        converted_at="${CONVERTED_AT:-$(date -u +%Y-%m-%dT%H:%M:%SZ)}"

        echo "Reporting booking ${booking_id} (${value} ${currency}) at ${converted_at}"
        post "/api/v1/conversions" "$(printf \
            '{"clickToken":"%s","bookingId":"%s","value":%s,"currency":"%s","convertedAt":"%s"}' \
            "$click_token" "$booking_id" "$value" "$currency" "$converted_at")"
        ;;

    cancel)
        [[ $# -lt 2 ]] && usage
        click_token="$1"
        booking_id="$2"

        echo "Cancelling booking ${booking_id}"
        post "/api/v1/conversions/cancellations" "$(printf \
            '{"clickToken":"%s","bookingId":"%s"}' \
            "$click_token" "$booking_id")"
        ;;

    *)
        usage
        ;;
esac