import React, { useState, useEffect } from 'react';
import {
  Box,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TablePagination,
  Chip,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  MenuItem,
  Typography,
  CircularProgress
} from '@mui/material';
import { orderService } from '../services/api';

const OrdersPage = () => {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [totalElements, setTotalElements] = useState(0);
  const [selectedOrder, setSelectedOrder] = useState(null);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [statusUpdateOpen, setStatusUpdateOpen] = useState(false);
  const [newStatus, setNewStatus] = useState('');

  const orderStatuses = [
    'PENDING',
    'CONFIRMED', 
    'PROCESSING',
    'SHIPPED',
    'DELIVERED',
    'CANCELLED'
  ];

  useEffect(() => {
    loadOrders();
  }, [page, rowsPerPage]);

  const loadOrders = async () => {
    try {
      setLoading(true);
      const response = await orderService.getOrders(page, rowsPerPage);
      setOrders(response.data.content || []);
      setTotalElements(response.data.totalElements || 0);
    } catch (error) {
      console.error('Error loading orders:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleChangePage = (event, newPage) => {
    setPage(newPage);
  };

  const handleChangeRowsPerPage = (event) => {
    setRowsPerPage(parseInt(event.target.value, 10));
    setPage(0);
  };

  const handleViewOrder = async (orderId) => {
    try {
      const response = await orderService.getOrder(orderId);
      setSelectedOrder(response.data);
      setDialogOpen(true);
    } catch (error) {
      console.error('Error loading order details:', error);
    }
  };

  const handleUpdateStatus = (order) => {
    setSelectedOrder(order);
    setNewStatus(order.status);
    setStatusUpdateOpen(true);
  };

  const handleStatusUpdate = async () => {
    try {
      await orderService.updateOrderStatus(selectedOrder.id, newStatus);
      setStatusUpdateOpen(false);
      loadOrders(); // Refresh the list
    } catch (error) {
      console.error('Error updating order status:', error);
    }
  };

  const handleCancelOrder = async (orderId) => {
    try {
      await orderService.cancelOrder(orderId);
      loadOrders(); // Refresh the list
    } catch (error) {
      console.error('Error cancelling order:', error);
    }
  };

  const getStatusColor = (status) => {
    const colors = {
      PENDING: 'warning',
      CONFIRMED: 'info',
      PROCESSING: 'primary',
      SHIPPED: 'secondary',
      DELIVERED: 'success',
      CANCELLED: 'error'
    };
    return colors[status] || 'default';
  };

  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD'
    }).format(amount);
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleString();
  };

  if (loading && orders.length === 0) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Orders Management
      </Typography>

      <Paper>
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Order ID</TableCell>
                <TableCell>Customer</TableCell>
                <TableCell>Total Amount</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Created Date</TableCell>
                <TableCell>Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {orders.map((order) => (
                <TableRow key={order.id}>
                  <TableCell>#{order.id}</TableCell>
                  <TableCell>{order.user?.name || 'N/A'}</TableCell>
                  <TableCell>{formatCurrency(order.totalAmount)}</TableCell>
                  <TableCell>
                    <Chip 
                      label={order.status} 
                      color={getStatusColor(order.status)}
                      size="small"
                    />
                  </TableCell>
                  <TableCell>{formatDate(order.createdAt)}</TableCell>
                  <TableCell>
                    <Button 
                      size="small" 
                      onClick={() => handleViewOrder(order.id)}
                      sx={{ mr: 1 }}
                    >
                      View
                    </Button>
                    <Button 
                      size="small" 
                      onClick={() => handleUpdateStatus(order)}
                      sx={{ mr: 1 }}
                    >
                      Update Status
                    </Button>
                    {order.status !== 'CANCELLED' && order.status !== 'DELIVERED' && (
                      <Button 
                        size="small" 
                        color="error"
                        onClick={() => handleCancelOrder(order.id)}
                      >
                        Cancel
                      </Button>
                    )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
        
        <TablePagination
          component="div"
          count={totalElements}
          page={page}
          onPageChange={handleChangePage}
          rowsPerPage={rowsPerPage}
          onRowsPerPageChange={handleChangeRowsPerPage}
          rowsPerPageOptions={[5, 10, 25, 50]}
        />
      </Paper>

      {/* Order Details Dialog */}
      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>Order Details</DialogTitle>
        <DialogContent>
          {selectedOrder && (
            <Box>
              <Typography variant="h6" gutterBottom>
                Order #{selectedOrder.id}
              </Typography>
              <Typography><strong>Customer:</strong> {selectedOrder.user?.name}</Typography>
              <Typography><strong>Email:</strong> {selectedOrder.user?.email}</Typography>
              <Typography><strong>Status:</strong> {selectedOrder.status}</Typography>
              <Typography><strong>Total Amount:</strong> {formatCurrency(selectedOrder.totalAmount)}</Typography>
              <Typography><strong>Created:</strong> {formatDate(selectedOrder.createdAt)}</Typography>
              
              <Typography variant="h6" sx={{ mt: 2, mb: 1 }}>
                Order Items
              </Typography>
              <TableContainer component={Paper} variant="outlined">
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Product</TableCell>
                      <TableCell>Quantity</TableCell>
                      <TableCell>Price</TableCell>
                      <TableCell>Total</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {selectedOrder.orderItems?.map((item, index) => (
                      <TableRow key={index}>
                        <TableCell>{item.product?.name}</TableCell>
                        <TableCell>{item.quantity}</TableCell>
                        <TableCell>{formatCurrency(item.price)}</TableCell>
                        <TableCell>{formatCurrency(item.quantity * item.price)}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>Close</Button>
        </DialogActions>
      </Dialog>

      {/* Status Update Dialog */}
      <Dialog open={statusUpdateOpen} onClose={() => setStatusUpdateOpen(false)}>
        <DialogTitle>Update Order Status</DialogTitle>
        <DialogContent>
          <TextField
            select
            fullWidth
            label="New Status"
            value={newStatus}
            onChange={(e) => setNewStatus(e.target.value)}
            sx={{ mt: 2 }}
          >
            {orderStatuses.map((status) => (
              <MenuItem key={status} value={status}>
                {status}
              </MenuItem>
            ))}
          </TextField>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setStatusUpdateOpen(false)}>Cancel</Button>
          <Button onClick={handleStatusUpdate} variant="contained">
            Update
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default OrdersPage;
