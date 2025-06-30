import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import MenuPage from './pages/MenuPage';
import OrderConfirmationPage from './pages/OrderConfirmationPage';
import OrderStatusPage from './pages/OrderStatusPage';

function App() {
  return (
    <Router>
      <div className="App">
        <Routes>
          <Route path="/menu/:entityId" element={<MenuPage />} />
          <Route path="/menu/table/:tableId" element={<MenuPage />} />
          <Route path="/tables/:tableId/menu" element={<MenuPage />} />
          <Route path="/menu" element={<MenuPage />} />
          <Route path="/order/confirmation/:orderNumber" element={<OrderConfirmationPage />} />
          <Route path="/order/status/:orderNumber" element={<OrderStatusPage />} />
          <Route path="/" element={
            <div className="container p-6 text-center">
              <h1 className="text-3xl font-bold mb-4">Restaurant Menu System</h1>
              <p className="text-gray-600">Please scan a QR code from your table to view the menu.</p>
            </div>
          } />
        </Routes>
      </div>
    </Router>
  );
}

export default App;
