import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import "./Home.css";
import axios from "axios";

const laptopData = [
  {
    id: 1,
    name: "Laptop A",
    price: "$1,000",
    imageUrl: "https://via.placeholder.com/150",
  },
  {
    id: 2,
    name: "Laptop B",
    price: "$1,200",
    imageUrl: "https://via.placeholder.com/150",
  },
  {
    id: 3,
    name: "Laptop C",
    price: "$1,500",
    imageUrl: "https://via.placeholder.com/150",
  },
];

interface Laptop {
  id: number;
  name: string;
  price: string;
  imageUrl: string;
}

function Home() {
  const [products, setProducts] = useState();
  const [productsToDay, setProductsToDay] = useState();
  const navigate = useNavigate();

  const handleProductClick = (id: number) => {
    navigate(`/product/${id}`);
  };

  return (
    <div
      style={{
        padding: "20px",
        fontFamily: "Arial, sans-serif",
        minHeight: "300px",
      }}
    >
      <h1>Thống Kê</h1>
      {/* <div>
        <label htmlFor="date">Chọn Ngày: </label>
        <input
          type="date"
          id="date"
          value={date}
          onChange={(e) => setDate(e.target.value)}
        />
      </div> */}

      <div style={{ marginTop: "20px", display:"flex", justifyContent:"space-around" }}>
        <div className="info">
          <p>
            <strong>Giá bán trung bình:</strong> 1 VND
          </p>
          <p>
            <strong>Số lượng sản phẩm:</strong> 2
          </p>
          <p>
            <strong>Số lượng nhãn hiệu:</strong> 2
          </p>
        </div>
        <div className="right">
          <p>
            <strong>Sản phẩm giá cao nhất</strong>
          </p>
          <img style={{width:"80px", height:"80px", marginTop:"10px"}} src="/imgs/laptop.png" alt="" /> <br />
          <b>DELL PRO_2024</b> <br />
          <b style={{fontWeight:"700", fontSize:"20px", color:"blue", borderBottom:"2px solid blue", paddingBottom:"10px"}}>40.000.000 VND</b> <br /><br />
          <b>DELL</b>
        </div>
        <div className="right">
          <p>
            <strong>Sản phẩm giá thấp nhất</strong>
            <img style={{width:"80px", height:"80px", marginTop:"10px"}} src="/imgs/laptop.png" alt="" /> <br />
          <b>ASUS PRO_2023</b> <br />
          <b style={{fontWeight:"700", fontSize:"20px", color:"blue", borderBottom:"2px solid blue", paddingBottom:"10px"}}>10.000.000 VND</b> <br /><br />
          <b>ASUS</b>
          </p>
        </div>
      </div>
    </div>
  );
}

export default Home;
