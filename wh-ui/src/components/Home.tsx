import React from 'react';
import { useNavigate } from 'react-router-dom';
import './Home.css';

const laptopData = [
  { id: 1, name: "Laptop A", price: "$1,000", imageUrl: "https://via.placeholder.com/150" },
  { id: 2, name: "Laptop B", price: "$1,200", imageUrl: "https://via.placeholder.com/150" },
  { id: 3, name: "Laptop C", price: "$1,500", imageUrl: "https://via.placeholder.com/150" },
];

function Home() {
  const navigate = useNavigate();

  const handleProductClick = (id: number) => {
    navigate(`/product/${id}`);
  };

  return (
    <div className="container">
      <h1>Danh sách Laptop</h1>
      <div className="product-grid">
        {laptopData.map((laptop) => (
          <div className="product-card" key={laptop.id} onClick={() => handleProductClick(laptop.id)}>
            <img src={laptop.imageUrl} alt={laptop.name} className="product-image" />
            <div className="product-info">
              <h2 className="product-name">{laptop.name}</h2>
              <p className="product-price">{laptop.price}</p>
              <button className="add-to-cart">Thêm vào giỏ hàng</button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

export default Home;