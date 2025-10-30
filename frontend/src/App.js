import React, { useState, useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Link } from 'react-router-dom';
import { AppBar, Toolbar, Typography, Container, Box, Tabs, Tab } from '@mui/material';
import Dashboard from './components/Dashboard';
import OrdersPage from './components/OrdersPage';
import ProductsPage from './components/ProductsPage';
import './App.css';

function App() {
  const [currentTab, setCurrentTab] = useState(0);

  const handleTabChange = (event, newValue) => {
    setCurrentTab(newValue);
  };

  return (
    <Router>
      <div className="App">
        <AppBar position="static" sx={{ backgroundColor: '#1976d2' }}>
          <Toolbar>
            <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
              Order Analytics Dashboard
            </Typography>
          </Toolbar>
        </AppBar>
        
        <Container maxWidth="xl" sx={{ mt: 2 }}>
          <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 3 }}>
            <Tabs value={currentTab} onChange={handleTabChange}>
              <Tab label="Dashboard" component={Link} to="/" />
              <Tab label="Orders" component={Link} to="/orders" />
              <Tab label="Products" component={Link} to="/products" />
            </Tabs>
          </Box>

          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/orders" element={<OrdersPage />} />
            <Route path="/products" element={<ProductsPage />} />
          </Routes>
        </Container>
      </div>
    </Router>
  );
}

export default App;
