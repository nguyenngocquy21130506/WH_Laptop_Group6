import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import "./Home.css";
import axios from "axios";

interface Laptop {
  id: number;
  products_name: string;
  price: number;
  brand: string;
  description: string;
  discount: number;
}

function Home() {
  const [products, setProducts] = useState<Laptop[]>([]);
  const [currentPage, setCurrentPage] = useState(1);
  const [hasMoreProducts, setHasMoreProducts] = useState(true);
  const productsPerPage = 10; // Số sản phẩm trên mỗi trang
  const navigate = useNavigate();

  const handleProductClick = (id: number) => {
    navigate(`/product/${id}`);
  };

  // Fetch product data from the API
  const fetchProducts = (page: number) => {
    const url = `http://localhost:8080/api/productsToDay/${page}`;
    console.log('url: ', url)
    axios
      .post(url)
      .then((response) => {
        const newProducts = response.data || [];
        setProducts(response.data); // Ensure API returns products in a `products` field
        // Nếu số sản phẩm ít hơn productsPerPage, không có trang tiếp theo
        setHasMoreProducts(newProducts.length === productsPerPage);
      })
      .catch((error) => {
        console.error("There was an error fetching the product data!", error);
        setHasMoreProducts(false);
      });
  };

  // Fetch products when the component mounts or the page changes
  useEffect(() => {
    fetchProducts(currentPage);
  }, [currentPage]);

  const handleNextPage = () => {
    if (hasMoreProducts) {
      setCurrentPage((prevPage) => prevPage + 1);
    }
  };

  const handlePreviousPage = () => {
    if (currentPage > 1) {
      setCurrentPage((prevPage) => prevPage - 1);
    }
  };

  return (
    <div className="container">
      <h1>Danh sách Laptop</h1>
      <div className="product-grid">
        {products.length > 0 ? (
          products.map((laptop) => (
            <div className="product-card"
            key={laptop.id}
            onClick={() => handleProductClick(laptop.id)}
            >
              <img src="/imgs/laptop.png" alt="" className="product-image" />
              <div className="product-info">
                <b
                  style={{
                    width: "150px",
                    height: "30px",
                    whiteSpace: "nowrap",
                    overflow: "hidden",
                    textOverflow: "ellipsis",
                  }}
                >
                  {laptop.products_name}
                </b>
                <p className="price">
                  {new Intl.NumberFormat("vi-VN", {
                    style: "currency",
                    currency: "VND",
                  }).format(laptop.price)}
                </p>
                <button className="add-to-cart" onClick={() => handleProductClick(laptop.id)}>Chi tiết sản phẩm</button>
              </div>
            </div>
          ))
        ) : (
          <p>Loading products...</p> // Show loading text while waiting for API response
        )}
      </div>

      {/* Pagination */}
      <div className="pagination">
        <button
          className="page-button"
          disabled={currentPage === 1}
          onClick={handlePreviousPage}
        >
          Trở lại
        </button>
        <span style={{marginTop:"10px", padding:"0 20px"}}>Trang {currentPage}</span>
        <button
          className="page-button"
          disabled={!hasMoreProducts}
          onClick={handleNextPage}
        >
          Tiếp
        </button>
      </div>
    </div>
  );
}

export default Home;
