#!/usr/bin/env bash
# サーバー初回セットアップ（root で 1 回だけ実行する）
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
APP_DIR=/opt/otetsudai
DOMAIN=hitsuji-design.work
CERT_EMAIL=hiroto.tsuji@hitsuji.design

# ===== アプリ用ユーザー =====
if ! id -u otetsudai &>/dev/null; then
    useradd --system --no-create-home --shell /usr/sbin/nologin otetsudai
    echo "ユーザー otetsudai を作成しました"
fi

# ===== ディレクトリ =====
mkdir -p "$APP_DIR/data" "$APP_DIR/logs"
chown -R otetsudai:otetsudai "$APP_DIR"
chmod 750 "$APP_DIR/data" "$APP_DIR/logs"
echo "ディレクトリ $APP_DIR を作成しました"

# ===== systemd サービス =====
cp "$SCRIPT_DIR/otetsudai.service" /etc/systemd/system/otetsudai.service
systemctl daemon-reload
systemctl enable otetsudai
echo "systemd サービスを登録しました"

# ===== Nginx + Let's Encrypt =====
# certbot のインストール確認
if ! command -v certbot &>/dev/null; then
    echo "certbot をインストール中..."
    apt-get update -q
    apt-get install -y certbot python3-certbot-nginx
fi

# 一時的な HTTP 設定でドメインの疎通を確認し、証明書を取得
cat > /etc/nginx/sites-available/otetsudai << 'NGINX_EOF'
server {
    listen 80;
    server_name hitsuji-design.work;
    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
NGINX_EOF

ln -sf /etc/nginx/sites-available/otetsudai /etc/nginx/sites-enabled/otetsudai

# デフォルトサイトを無効化（ポート 80 競合を防ぐ）
rm -f /etc/nginx/sites-enabled/default

nginx -t
systemctl reload nginx
echo "Nginx に一時設定を適用しました"

# certbot で証明書を取得（--nginx プラグインが自動で設定を書き換える）
certbot --nginx \
    -d "$DOMAIN" \
    --non-interactive \
    --agree-tos \
    -m "$CERT_EMAIL"
echo "Let's Encrypt 証明書を取得しました"

# 本番用 Nginx 設定（セキュリティヘッダー等）を上書き
cp "$SCRIPT_DIR/nginx.conf" /etc/nginx/sites-available/otetsudai
nginx -t
systemctl reload nginx
echo "Nginx に本番設定を適用しました"

echo ""
echo "セットアップ完了。deploy.sh を実行してアプリをデプロイしてください。"
echo "  アクセス先: https://$DOMAIN"
