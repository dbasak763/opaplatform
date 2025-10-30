# Frontend Dashboard

React-based admin dashboard for the Order Processing System with real-time analytics and order management.

## Features

- **Real-time Dashboard**: Live metrics with WebSocket updates
- **Order Management**: View, update, and cancel orders
- **Product Analytics**: Top products and performance metrics
- **Interactive Charts**: Revenue trends, order status distribution
- **Responsive Design**: Material-UI components with mobile support

## Tech Stack

- **React 18** - Frontend framework
- **Material-UI** - Component library
- **Chart.js** - Data visualization
- **Axios** - HTTP client
- **WebSocket** - Real-time updates

## Pages

### Dashboard (`/`)
- Key metrics cards (orders, revenue, avg order value)
- Revenue trend chart (24 hours)
- Order status distribution (doughnut chart)
- Orders per hour (bar chart)
- Recent orders list

### Orders (`/orders`)
- Paginated orders table
- Order status management
- Order details modal
- Cancel order functionality

### Products (`/products`)
- Top performing products
- Product catalog table
- Sales metrics

## API Integration

### Order Service (Port 8090)
- Authentication: Basic Auth (admin:admin123)
- Endpoints: Orders, Products, Users, Analytics

### Analytics Service (Port 8091)
- Real-time metrics and statistics
- WebSocket for live updates
- Historical analytics data

## Setup & Running

### Development
```bash
# Install dependencies
npm install

# Start development server
npm start
```

### Production Build
```bash
# Build for production
npm run build

# Serve production build
npm install -g serve
serve -s build
```

### Docker
```bash
# Build and run with Docker Compose
docker-compose up frontend
```

## Environment Variables

- `REACT_APP_ORDER_SERVICE_URL` - Order service URL
- `REACT_APP_ANALYTICS_SERVICE_URL` - Analytics service URL

## Components

- `Dashboard.js` - Main analytics dashboard
- `OrdersPage.js` - Order management interface
- `ProductsPage.js` - Product analytics and catalog
- `services/api.js` - API service layer

## Real-time Features

The dashboard connects to the analytics service via WebSocket for:
- Live order metrics updates
- Real-time revenue tracking
- Recent orders feed
- Active user counts
