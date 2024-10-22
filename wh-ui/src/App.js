import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Home from './components/Home.tsx';
import ProductDetail from './components/Detail.tsx';
import Header from './components/Header.tsx';
import Footer from './components/Footer.tsx';
import './App.css';

function App() {
  return (
    <Router>
      <div className="App">
        <Header />
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/product/:id" element={<ProductDetail />} />
        </Routes>
        <Footer />
      </div>
    </Router>
  );
}

export default App;
