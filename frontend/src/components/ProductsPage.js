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
  Typography,
  CircularProgress,
  Grid,
  Card,
  CardContent
} from '@mui/material';
import { orderService, analyticsService } from '../services/api';

const ProductsPage = () => {
  const [products, setProducts] = useState([]);
  const [topProducts, setTopProducts] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadProductsData();
  }, []);

  const loadProductsData = async () => {
    try {
      setLoading(true);
      
      // Load products from order service
      const productsResponse = await orderService.getProducts();
      setProducts(productsResponse.data);

      // Try to load top products from analytics service
      try {
        const topProductsResponse = await analyticsService.getTopProducts(5);
        setTopProducts(topProductsResponse.data.products);
      } catch (error) {
        console.log('Analytics service not available, using mock data');
        // Mock top products data
        setTopProducts([
          {
            product_id: "1",
            product_name: "Wireless Headphones",
            total_quantity_sold: 150,
            total_revenue: 14999.50,
            order_count: 75
          },
          {
            product_id: "2",
            product_name: "Phone Case",
            total_quantity_sold: 200,
            total_revenue: 3999.00,
            order_count: 100
          }
        ]);
      }
      
    } catch (error) {
      console.error('Error loading products data:', error);
    } finally {
      setLoading(false);
    }
  };

  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD'
    }).format(amount);
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
      <Typography variant="h4" gutterBottom>
        Products Management
      </Typography>

      {/* Top Products Section */}
      <Typography variant="h5" gutterBottom sx={{ mt: 3 }}>
        Top Performing Products
      </Typography>
      <Grid container spacing={3} sx={{ mb: 4 }}>
        {topProducts.map((product, index) => (
          <Grid item xs={12} sm={6} md={4} key={product.product_id}>
            <Card>
              <CardContent>
                <Typography variant="h6" gutterBottom>
                  #{index + 1} {product.product_name}
                </Typography>
                <Typography variant="body2" color="textSecondary">
                  Revenue: {formatCurrency(product.total_revenue)}
                </Typography>
                <Typography variant="body2" color="textSecondary">
                  Quantity Sold: {product.total_quantity_sold}
                </Typography>
                <Typography variant="body2" color="textSecondary">
                  Orders: {product.order_count}
                </Typography>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      {/* All Products Table */}
      <Typography variant="h5" gutterBottom>
        All Products
      </Typography>
      <Paper>
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>ID</TableCell>
                <TableCell>Name</TableCell>
                <TableCell>Description</TableCell>
                <TableCell>Price</TableCell>
                <TableCell>Stock Quantity</TableCell>
                <TableCell>Category</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {products.map((product) => (
                <TableRow key={product.id}>
                  <TableCell>#{product.id}</TableCell>
                  <TableCell>{product.name}</TableCell>
                  <TableCell>{product.description}</TableCell>
                  <TableCell>{formatCurrency(product.price)}</TableCell>
                  <TableCell>{product.stockQuantity}</TableCell>
                  <TableCell>{product.category}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      </Paper>
    </Box>
  );
};

export default ProductsPage;
