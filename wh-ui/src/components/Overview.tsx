import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import axios from "axios";
import "./Home.css";

// Định nghĩa kiểu dữ liệu Aggregate
interface Aggregate {
  avg_price: number;
  min_price: number;
  max_price: number;
  total_products: number;
  min_price_product_name: string;
  max_price_product_name: string;
}

function Home() {
  // State để lưu trữ dữ liệu Aggregate
  const [aggregate, setAggregate] = useState<Aggregate | null>(null);
  const navigate = useNavigate();

  // Gọi API khi component được mount
  useEffect(() => {
    // Gọi API và lấy dữ liệu
    const fetchData = () => {
      const url = `http://localhost:8080/api/overview/overview`;
      console.log("url: ", url);
      try {
        axios.post(url).then((response) => {
          setAggregate(response.data); // Cập nhật state với dữ liệu trả về từ API
        });
      } catch (error) {
        console.error("Lỗi khi gọi API:", error);
      }
    };

    fetchData();
  }, []); // Hàm chỉ chạy một lần khi component mount

  // Xử lý khi người dùng click vào sản phẩm
  const handleProductClick = (id: number) => {
    navigate(`/product/${id}`);
  };

  // Kiểm tra xem dữ liệu đã có hay chưa để tránh lỗi render
  if (!aggregate) {
    return <div>Loading...</div>;
  }

  return (
    <div
      style={{
        padding: "20px",
        fontFamily: "Arial, sans-serif",
        minHeight: "300px",
      }}
    >
      <h1>Thống Kê</h1>
      <div
        style={{
          marginTop: "20px",
          display: "flex",
          justifyContent: "space-around",
        }}
      >
        <div className="info">
          <p>
            <strong>Giá bán trung bình mỗi sản phẩm:</strong>{" "}
            {new Intl.NumberFormat("vi-VN", {
                    style: "currency",
                    currency: "VND",
                  }).format(aggregate.avg_price)}
          </p>
          <p>
            <strong>Số lượng sản phẩm:</strong> {aggregate.total_products}
          </p>
          {/* <p>
            <strong>Số lượng thương hiệu:</strong>{" "}
          </p> */}
        </div>
        <div className="right">
          <p>
            <strong>Sản phẩm giá cao nhất</strong>
          </p>
          <img
            style={{ width: "80px", height: "80px", marginTop: "10px" }}
            src="/imgs/laptop.png"
            alt="Highest Price Product"
          />
          <br />
          <b className="productName" onClick={()=>handleProductClick(273716780)}>{aggregate.max_price_product_name}</b>
          <br />
          <b
            style={{
              fontWeight: "700",
              fontSize: "20px",
              color: "blue",
              borderBottom: "2px solid blue",
              paddingBottom: "10px",
            }}
          >
            {new Intl.NumberFormat("vi-VN", {
                    style: "currency",
                    currency: "VND",
                  }).format(aggregate.max_price)}
          </b>
          <br />
          <br />
          <b>HP</b>
        </div>
        <div className="right">
          <p>
            <strong>Sản phẩm giá thấp nhất</strong>
          </p>
          <img
            style={{ width: "80px", height: "80px", marginTop: "10px" }}
            src="/imgs/laptop.png"
            alt="Lowest Price Product"
          />
          <br />
          <b className="productName" onClick={()=>handleProductClick(273716812)}>{aggregate.min_price_product_name}</b>
          <br />
          <b
            style={{
              fontWeight: "700",
              fontSize: "20px",
              color: "blue",
              borderBottom: "2px solid blue",
              paddingBottom: "10px",
            }}
          >
            {new Intl.NumberFormat("vi-VN", {
                    style: "currency",
                    currency: "VND",
                  }).format(aggregate.min_price)}
          </b>
          <br />
          <br />
          <b>LENOVO</b>
        </div>
      </div>
    </div>
  );
}

export default Home;
