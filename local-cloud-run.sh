#!/bin/bash

echo "🚀 Setting up environment for bidrag-bidragskalkulator-api..."

# 🛑 Check if user is signed in to Google Cloud
if ! gcloud auth list --filter=status:ACTIVE --format="value(account)" | grep -q "@"; then
    echo "🔑 You are not signed in to Google Cloud. Please sign in..."
    gcloud auth login
    if [ $? -ne 0 ]; then
        echo "❌ Google Cloud authentication failed. Exiting..."
        exit 1
    fi
else
    echo "✅ You are already signed in to Google Cloud."
fi

# 🔧 Ensure kubectl is configured for the correct cluster and namespace
echo "🔧 Configuring kubectl..."
kubectl config use-context dev-gcp
kubectl config set-context --current --namespace=bidrag

# Define the properties file path
SECRETS_FILE="src/test/resources/application-local-nais.properties"

# 🔍 Check if the secrets file already exists
if [ -f "$SECRETS_FILE" ]; then
    read -p "⚠️ $SECRETS_FILE already exists. Do you want to overwrite it? (y/n): " choice
    case "$choice" in
      y|Y ) echo "🔄 Overwriting $SECRETS_FILE...";;
      n|N ) echo "✅ Keeping the existing $SECRETS_FILE."; exit 0;;
      * ) echo "❌ Invalid input. Exiting."; exit 1;;
    esac
fi

# 🔑 Fetch secrets and store them in the properties file
echo "🔑 Fetching secrets..."
kubectl exec --tty deployment/bidrag-bidragskalkulator-api-feature -- printenv \
  | grep -E 'TOKEN_X_WELL_KNOWN_URL|TOKEN_X_CLIENT_ID' \
  > "$SECRETS_FILE"

echo "✅ Secrets saved to $SECRETS_FILE"

# 🛑 Ensure the file is ignored by Git
if ! grep -q "$SECRETS_FILE" .gitignore; then
    echo "🚨 Adding $SECRETS_FILE to .gitignore..."
    echo "$SECRETS_FILE" >> .gitignore
fi

# 🚀 Start the application
echo "🚀 Starting application with local-nais profile..."
./gradlew bootRun --args='--spring.profiles.active=local-nais'