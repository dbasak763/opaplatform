import React, { useState, useEffect } from 'react';
import { Grid, Paper, Typography, Box, CircularProgress, ToggleButton, ToggleButtonGroup } from '@mui/material';
import { Line, Doughnut, Bar } from 'react-chartjs-2';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
  ArcElement,
  BarElement,
} from 'chart.js';
import { orderService, analyticsService, RealtimeConnection } from '../services/api';

ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
  ArcElement,
  BarElement
);

const Dashboard = () => {
  const [metrics, setMetrics] = useState(null);
  const [realtimeStats, setRealtimeStats] = useState(null);
  const [trendCache, setTrendCache] = useState({});
  const [revenueTrend, setRevenueTrend] = useState({ interval: 'hour', data: [] });
  const [ordersTrend, setOrdersTrend] = useState({ interval: 'hour', data: [] });
  const [revenueInterval, setRevenueInterval] = useState('hour');
  const [ordersInterval, setOrdersInterval] = useState('hour');
  const [loading, setLoading] = useState(true);
  const [wsConnection, setWsConnection] = useState(null);

  useEffect(() => {
    loadInitialData();
    setupRealtimeConnection();

    return () => {
      if (wsConnection) {
        wsConnection.disconnect();
      }
    };
  }, []);

  const loadInitialData = async () => {
    try {
      setLoading(true);
      
      // Load data from both services
      const [metricsRes, realtimeRes, trendsRes] = await Promise.allSettled([
        analyticsService.getOrderMetrics(),
        analyticsService.getRealtimeStats(),
        analyticsService.getTrends('hour', 24)
      ]);

      if (metricsRes.status === 'fulfilled') {
        setMetrics(metricsRes.value.data);
      }
      
      if (realtimeRes.status === 'fulfilled') {
        setRealtimeStats(realtimeRes.value.data);
      }
      
      if (trendsRes.status === 'fulfilled') {
        const payload = trendsRes.value.data;
        const intervalKey = payload?.interval || 'hour';
        setTrendCache(prev => ({ ...prev, [intervalKey]: payload }));
        setRevenueTrend(payload);
        setOrdersTrend(payload);
      }

      // Fallback to order service if analytics service is not available
      if (metricsRes.status === 'rejected') {
        try {
          const [revenueRes, ordersRes] = await Promise.all([
            orderService.getRevenue(),
            orderService.getOrders(0, 100)
          ]);
          
          const fallbackMetrics = {
            total_orders: ordersRes.data.totalElements || 0,
            total_revenue: revenueRes.data.totalRevenue || 0,
            avg_order_value: revenueRes.data.totalRevenue / (ordersRes.data.totalElements || 1),
            orders_by_status: {}
          };
          
          setMetrics(fallbackMetrics);
        } catch (error) {
          console.error('Error loading fallback data:', error);
        }
      }
      
    } catch (error) {
      console.error('Error loading dashboard data:', error);
    } finally {
      setLoading(false);
    }
  };

  const setupRealtimeConnection = () => {
    const connection = new RealtimeConnection(
      (data) => {
        setRealtimeStats(data);
      },
      (error) => {
        console.error('WebSocket error:', error);
      }
    );
    
    connection.connect();
    setWsConnection(connection);
  };

  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD'
    }).format(amount);
  };

  const currentMetrics = metrics || {};

  const filteredRevenueBuckets = (revenueTrend?.data || []).filter(item => item && item.revenue !== undefined);
  const filteredOrderBuckets = (ordersTrend?.data || []).filter(item => item && item.orders !== undefined);

  const revenueChartData = {
    labels: filteredRevenueBuckets.map(item => item.label),
    datasets: [
      {
        label: revenueInterval === 'minute' ? 'Revenue per Minute' : 'Revenue per Hour',
        data: filteredRevenueBuckets.map(item => item.revenue),
        borderColor: 'rgb(75, 192, 192)',
        backgroundColor: 'rgba(75, 192, 192, 0.2)',
        tension: 0.1
      }
    ]
  };

  const statusChartData = {
    labels: Object.keys(metrics?.orders_by_status || {}),
    datasets: [
      {
        data: Object.values(metrics?.orders_by_status || {}),
        backgroundColor: [
          '#FF6384',
          '#36A2EB',
          '#FFCE56',
          '#4BC0C0',
          '#9966FF',
          '#FF9F40'
        ]
      }
    ]
  };

  const ordersChartData = {
    labels: filteredOrderBuckets.map(item => item.label),
    datasets: [
      {
        label: ordersInterval === 'minute' ? 'Orders per Minute' : 'Orders per Hour',
        data: filteredOrderBuckets.map(item => item.orders),
        backgroundColor: 'rgba(54, 162, 235, 0.5)',
        borderColor: 'rgba(54, 162, 235, 1)',
        borderWidth: 1
      }
    ]
  };

  const fetchTrendData = async (interval) => {
    if (trendCache[interval]) {
      return trendCache[interval];
    }

    const response = await analyticsService.getTrends(interval, interval === 'minute' ? 60 : 24);
    const payload = response?.data || { interval, data: [] };
    setTrendCache(prev => ({ ...prev, [interval]: payload }));
    return payload;
  };

  const handleTrendIntervalChange = async (event, nextInterval, target) => {
    if (!nextInterval) {
      return;
    }

    if (target === 'revenue' && nextInterval === revenueInterval) {
      return;
    }

    if (target === 'orders' && nextInterval === ordersInterval) {
      return;
    }

    if (target === 'revenue') {
      setRevenueInterval(nextInterval);
    } else {
      setOrdersInterval(nextInterval);
    }

    try {
      const payload = await fetchTrendData(nextInterval);
      if (target === 'revenue') {
        setRevenueTrend(payload);
      } else {
        setOrdersTrend(payload);
      }
    } catch (error) {
      console.error('Error loading trend data:', error);
    }
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box>
      {/* Key Metrics */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={6} md={3}>
          <Paper sx={{ p: 2, textAlign: 'center' }}>
            <Typography variant="h4" color="primary">
              {metrics?.total_orders || 0}
            </Typography>
            <Typography variant="body2" color="textSecondary">
              Total Orders
            </Typography>
          </Paper>
        </Grid>
        
        <Grid item xs={12} sm={6} md={3}>
          <Paper sx={{ p: 2, textAlign: 'center' }}>
            <Typography variant="h4" color="primary">
              {formatCurrency(metrics?.total_revenue || 0)}
            </Typography>
            <Typography variant="body2" color="textSecondary">
              Total Revenue
            </Typography>
          </Paper>
        </Grid>
        
        <Grid item xs={12} sm={6} md={3}>
          <Paper sx={{ p: 2, textAlign: 'center' }}>
            <Typography variant="h4" color="primary">
              {formatCurrency(metrics?.avg_order_value || 0)}
            </Typography>
            <Typography variant="body2" color="textSecondary">
              Avg Order Value
            </Typography>
          </Paper>
        </Grid>
        
        <Grid item xs={12} sm={6} md={3}>
          <Paper sx={{ p: 2, textAlign: 'center' }}>
            <Typography variant="h4" color="primary">
              {realtimeStats?.current_orders_per_minute?.toFixed(1) || '0.0'}
            </Typography>
            <Typography variant="body2" color="textSecondary">
              Orders/Minute
            </Typography>
          </Paper>
        </Grid>
      </Grid>

      {/* Charts */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid item xs={12} md={8}>
          <Paper sx={{ p: 2 }}>
            <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
              <Typography variant="h6">
                Revenue Trend
              </Typography>
              <ToggleButtonGroup
                value={revenueInterval}
                exclusive
                size="small"
                onChange={(event, value) => handleTrendIntervalChange(event, value, 'revenue')}
              >
                <ToggleButton value="hour">Hourly</ToggleButton>
                <ToggleButton value="minute">Per Minute</ToggleButton>
              </ToggleButtonGroup>
            </Box>
            <Line data={revenueChartData} options={{ responsive: true }} />
          </Paper>
        </Grid>
        
        <Grid item xs={12} md={4}>
          <Paper sx={{ p: 2 }}>
            <Typography variant="h6" gutterBottom>
              Orders by Status
            </Typography>
            <Doughnut data={statusChartData} options={{ responsive: true }} />
          </Paper>
        </Grid>
      </Grid>

      <Grid container spacing={3}>
        <Grid item xs={12} md={8}>
          <Paper sx={{ p: 2 }}>
            <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
              <Typography variant="h6">
                Orders Trend
              </Typography>
              <ToggleButtonGroup
                value={ordersInterval}
                exclusive
                size="small"
                onChange={(event, value) => handleTrendIntervalChange(event, value, 'orders')}
              >
                <ToggleButton value="hour">Hourly</ToggleButton>
                <ToggleButton value="minute">Per Minute</ToggleButton>
              </ToggleButtonGroup>
            </Box>
            <Bar data={ordersChartData} options={{ responsive: true }} />
          </Paper>
        </Grid>
        
        <Grid item xs={12} md={4}>
          <Paper sx={{ p: 2 }}>
            <Typography variant="h6" gutterBottom>
              Recent Orders
            </Typography>
            <Box sx={{ maxHeight: 300, overflow: 'auto' }}>
              {realtimeStats?.recent_orders?.slice(0, 5).map((order, index) => (
                <Box key={index} sx={{ p: 1, borderBottom: '1px solid #eee' }}>
                  <Typography variant="body2">
                    Order #{order.orderId}
                  </Typography>
                  <Typography variant="caption" color="textSecondary">
                    {formatCurrency(order.totalAmount)} - {order.eventType}
                  </Typography>
                </Box>
              )) || (
                <Typography variant="body2" color="textSecondary">
                  No recent orders
                </Typography>
              )}
            </Box>
          </Paper>
        </Grid>
      </Grid>
    </Box>
  );
};

export default Dashboard;
