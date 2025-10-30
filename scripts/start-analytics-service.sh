#!/bin/bash

# Start Analytics Service
echo "Starting Analytics Service..."

# Navigate to analytics service directory
cd "$(dirname "$0")/../analytics-service"

# Check if virtual environment exists, create if not
if [ ! -d "venv" ]; then
    echo "Creating Python virtual environment..."
    python3 -m venv venv
fi

# Activate virtual environment
source venv/bin/activate

# Install dependencies
echo "Installing Python dependencies..."
pip install -r requirements.txt

# Start the analytics service
echo "Starting Analytics Service on port 8091..."
python main.py
