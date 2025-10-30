#!/bin/bash

# Start Frontend Dashboard
echo "Starting Frontend Dashboard..."

# Navigate to frontend directory
cd "$(dirname "$0")/../frontend"

# Install dependencies if node_modules doesn't exist
if [ ! -d "node_modules" ]; then
    echo "Installing Node.js dependencies..."
    npm install
fi

# Start the React development server
echo "Starting React development server on port 3000..."
npm start
