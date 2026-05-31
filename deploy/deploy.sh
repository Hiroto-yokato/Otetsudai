#!/usr/bin/env bash
# ビルド & デプロイ（ローカルから実行する）
# 使い方: SERVER_HOST=user@your-server ./deploy/deploy.sh
set -euo pipefail

: "${SERVER_HOST:?環境変数 SERVER_HOST が未設定です。例: SERVER_HOST=ubuntu@203.0.113.1 ./deploy/deploy.sh}"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
JAR_SRC="$PROJECT_DIR/target/otetsudai-0.0.1-SNAPSHOT.jar"
REMOTE_JAR=/opt/otetsudai/otetsudai.jar

# Maven コマンドの解決（Windows の Git Bash では mvn.cmd が必要な場合がある）
MVN="${MVN_CMD:-mvn}"
if ! command -v mvn &>/dev/null && [[ -f "C:/tools/apache-maven-3.9.14/bin/mvn.cmd" ]]; then
    MVN="C:/tools/apache-maven-3.9.14/bin/mvn.cmd"
fi

echo "=== ビルド中 ==="
cd "$PROJECT_DIR"
"$MVN" clean package -DskipTests

echo ""
echo "=== サーバーに転送中 ($SERVER_HOST) ==="
scp "$JAR_SRC" "$SERVER_HOST:$REMOTE_JAR"

echo ""
echo "=== サービスを再起動中 ==="
ssh "$SERVER_HOST" "sudo systemctl restart otetsudai && sudo systemctl status otetsudai --no-pager"

echo ""
echo "デプロイ完了"
