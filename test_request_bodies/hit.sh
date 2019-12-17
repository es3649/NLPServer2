if [[ "$@" -lt "2" ]]; then
  echo "Usage: $0 [IP] [FILENAME]"
  echo "  where IP is the address to hit"
  echo "  and FILENAME contains the body for he request"
  exit 0
fi

curl -k -X POST https://$1/analyze \
   -H "Password:These APIs are Password-Protected, bruh" \
   --data "@$2"
