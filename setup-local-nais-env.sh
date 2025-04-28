#!/bin/bash

echo "🚀 Setting up environment for bidrag-bidragskalkulator-api..."

# 🔧 Ensure kubectl is configured for the correct cluster and namespace
echo "🔧 Configuring kubectl..."

# If necessary, install the gke-gcloud-auth-plugin
if ! command -v gke-gcloud-auth-plugin &> /dev/null; then
    echo "⚠️ gke-gcloud-auth-plugin is not installed. Installing it..."
    gcloud components install gke-gcloud-auth-plugin
fi

# Try to get the credentials for kubectl
kubectl config use-context dev-gcp
if ! kubectl config view; then
    echo "❌ Failed to configure kubectl. Ensure you are logged in to Google Cloud and have proper access."
    exit 1
fi

kubectl config set-context --current --namespace=bidrag

# Define the properties file path
SECRETS_FILE="src/main/resources/application-local-nais.properties"

# 🔍 Check if the secrets file already exists
if [ -f "$SECRETS_FILE" ]; then
    read -p "⚠️ $SECRETS_FILE already exists. Do you want to overwrite it? (y/n): " choice
    case "$choice" in
      y|Y ) echo "🔄 Overwriting $SECRETS_FILE...";;
      n|N ) echo "✅ Keeping the existing $SECRETS_FILE."; exit 0;;
      * ) echo "❌ Invalid input. Exiting..."; exit 1;;
    esac
fi

# 🔑 Fetch secrets and store them in the properties file
echo "🔑 Fetching secrets..."
kubectl exec --tty deployment/bidrag-bidragskalkulator-api -- printenv \
  | grep -E 'TOKEN_X_WELL_KNOWN_URL|TOKEN_X_CLIENT_ID|AZURE_APP_TENANT_ID|AZURE_APP_CLIENT_SECRET|AZURE_APP_CLIENT_ID|AZURE_APP_WELL_KNOWN_URL|AZURE_OPENID_CONFIG_TOKEN_ENDPOINT|BIDRAG_SJABLON_URL|BIDRAG_PERSON_URL|SCOPE|TOKEN_X|NAIS_APP_NAME|KODEVERK_URL|BIDRAG_GRUNNLAG_SCOPE|BIDRAG_GRUNNLAG_URL' \
  > "$SECRETS_FILE"

echo "✅ Secrets saved to $SECRETS_FILE"

# 🛑 Ensure the file is ignored by Git
if ! grep -q "$SECRETS_FILE" .gitignore; then
    echo "🚨 Adding $SECRETS_FILE to .gitignore..."
    echo "$SECRETS_FILE" >> .gitignore
fi