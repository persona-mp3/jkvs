#!/usr/bin/env bash
HOST="127.0.0.1"
PORT="9090"
limit=1000000

# Hot key configuration
HOT_KEY_COUNT=20        # 20 frequently accessed keys
COLD_KEY_COUNT=10000    # 10000 rarely accessed keys
HOT_KEY_RATIO=80        # 80% of requests go to hot keys

exec 3<>/dev/tcp/$HOST/$PORT

echo "Starting benchmark: $limit requests"
echo "Hot keys: $HOT_KEY_COUNT (${HOT_KEY_RATIO}% of traffic)"
echo "Cold keys: $COLD_KEY_COUNT ($((100 - HOT_KEY_RATIO))% of traffic)"
echo "Value sizes: small (~14B), medium (~136B), large (~1.3KB)"
echo "---"

for i in $(seq 0 $((limit - 1))); do
    # Zipfian distribution: hot vs cold keys
    rand=$((RANDOM % 100))
    
    if [ $rand -lt $HOT_KEY_RATIO ]; then
        # Hot keys - frequently accessed
        key="hot_key_$((RANDOM % HOT_KEY_COUNT))"
        key_type="HOT"
    else
        # Cold keys - rarely accessed
        key="cold_key_$((RANDOM % COLD_KEY_COUNT))"
        key_type="COLD"
    fi
    
    # Variable payload sizes
    size_type=$((RANDOM % 3))
    case $size_type in
        0)  # Small: ~14 bytes
            value=$(head -c 10 /dev/urandom 2>/dev/null | base64 | tr -d '\n')
            size_label="SMALL"
            ;;
        1)  # Medium: ~136 bytes
            value=$(head -c 100 /dev/urandom 2>/dev/null | base64 | tr -d '\n')
            size_label="MEDIUM"
            ;;
        2)  # Large: ~1.3 KB
            value=$(head -c 1000 /dev/urandom 2>/dev/null | base64 | tr -d '\n')
            size_label="LARGE"
            ;;
    esac
    
    # Add metadata to value for tracking
    value="${value}|meta:req=${i},size=${size_label},key_type=${key_type}"
    
    json=$(printf '{"command":"set","key":"%s","value":"%s"}' "$key" "$value")
    
    start_ns=$(date +%s%N)
    echo "$json" >&3
    IFS= read -r response <&3
    end_ns=$(date +%s%N)
    
    latency_ns=$((end_ns - start_ns))
    latency_us=$((latency_ns / 1000))
    
    # Output: request_num, latency, key_type, size, value_bytes, response
    echo "$i ${latency_us}us $key_type $size_label ${#value}B $response"
    
    # Progress indicator every 10k requests
    if [ $((i % 10000)) -eq 0 ] && [ $i -gt 0 ]; then
        echo "--- Progress: $i / $limit requests completed ---" >&2
    fi
done

exec 3<&-
exec 3>&-

echo "Benchmark complete: $limit requests sent"
