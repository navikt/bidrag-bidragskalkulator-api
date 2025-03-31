#!/bin/bash

echo "🚀 Setting up environment for bidrag-bidragskalkulator-api..."

# 🛑 Check if something is running on port 8080
echo "🔍 Checking if something is running on port 8080..."
PORT_8080_PID=$(lsof -t -i:8080)

if [ -n "$PORT_8080_PID" ]; then
    echo "⚠️ A process is already running on port 8080. Killing the process..."
    kill -9 "$PORT_8080_PID"
    echo "✅ Process on port 8080 killed."
else
    echo "✅ No process found running on port 8080."
fi

# 🛑 Check if user is signed in to Google Cloud
if ! gcloud auth list --filter=status:ACTIVE --format="value(account)" | grep -q "@"; then
    echo "🔑 You are not signed in to Google Cloud. Please sign in..."

    # Run gcloud auth login
    gcloud auth login

    # Check if login was successful
    if ! gcloud auth list --filter=status:ACTIVE --format="value(account)" | grep -q "@"; then
        echo "❌ Google Cloud authentication failed. Exiting..."
        exit 1
    fi
    echo "✅ Successfully logged in to Google Cloud."
else
    echo "✅ You are already signed in to Google Cloud."
fi

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
./gradlew bootRun --args='--spring.profiles.active=local-nais' & APP_PID=$!

# 🚀 Wait for the application to start up (adjust the time as needed)
echo "⏳ Waiting for the application to start..."
sleep 10  # Adjust the sleep time if needed (or replace with a more robust check)

# 🚀 Check if the app is responding
swagger_url="http://localhost:8080/swagger-ui/index.html"
if curl --silent --head --fail "$swagger_url" > /dev/null; then
    echo "✅ Application is running. Opening Swagger UI..."
    # Check the OS and open the URL in the default browser
    if [[ "$OSTYPE" == "linux-gnu"* ]]; then
        xdg-open "$swagger_url"
    elif [[ "$OSTYPE" == "darwin"* ]]; then
        open "$swagger_url"
    else
        echo "Unsupported OS"
    fi
else
    echo "❌ Application did not respond. Please check if it started correctly."
    exit 1
fi

# Get the TOKEN_X_CLIENT_ID from the secrets file
TOKEN_X_CLIENT_ID=$(grep -i 'TOKEN_X_CLIENT_ID' "$SECRETS_FILE" | sed 's/^[[:space:]]*TOKEN_X_CLIENT_ID=[[:space:]]*//g')

# Check if TOKEN_X_CLIENT_ID was found
if [ -z "$TOKEN_X_CLIENT_ID" ]; then
    echo "❌ TOKEN_X_CLIENT_ID not found in $SECRETS_FILE. Exiting..."
    exit 1
fi

# Replace <audience> in the token generator URL with the required format
tokenx_generator="https://tokenx-token-generator.intern.dev.nav.no/api/obo?aud=$TOKEN_X_CLIENT_ID"

# Open the tokenx_generator URL in the browser
echo "🚀 Opening TokenX Generator URL..."
if [[ "$OSTYPE" == "linux-gnu"* ]]; then
    xdg-open "$tokenx_generator"
elif [[ "$OSTYPE" == "darwin"* ]]; then
    open "$tokenx_generator"
else
    echo "Unsupported OS"
fi

# Function to stop the app if Ctrl+C is pressed
trap "echo '⚠️ Stopping the application...'; kill -9 $APP_PID; exit" INT

# Wait for the background process to finish (this keeps the script running)
wait $APP_PID