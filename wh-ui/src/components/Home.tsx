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
  const [searchTerm, setSearchTerm] = useState(""); // State cho tìm kiếm
  const productsPerPage = 10; // Số sản phẩm trên mỗi trang
  const navigate = useNavigate();

  const handleProductClick = (id: number) => {
    navigate(`/product/${id}`);
  };

  // Fetch product data from the API
  const fetchProducts = (page: number, searchTerm: string) => {
    const url = searchTerm
      ? `http://localhost:8080/api/searchProducts/${searchTerm}`
      : `http://localhost:8080/api/productsToDay/${page}`;
    console.log('url: ', url);
    axios.post(url)
      .then((response) => {
        const newProducts = response.data || [];
        console.log('newProducts: ', newProducts);
        setProducts(newProducts);
        setHasMoreProducts(newProducts.length === productsPerPage);
      })
      .catch((error) => {
        console.error("There was an error fetching the product data!", error);
        setHasMoreProducts(false);
      });
  };

  // Fetch products when the component mounts, page changes, or search term changes
  useEffect(() => {
    fetchProducts(currentPage, searchTerm);
  }, [currentPage, searchTerm]); // Thêm `searchTerm` vào dependency

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

  const handleSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSearchTerm(e.target.value); // Cập nhật từ khóa tìm kiếm
  };

  return (
    <div className="container">
      <h1>Danh sách Laptop</h1>

      {/* Thêm ô tìm kiếm */}
      <div className="search-bar">
        <input
          type="text"
          value={searchTerm}
          onChange={handleSearchChange}
          placeholder="Tìm kiếm sản phẩm..."
        />
      </div>
      
      <div className="product-grid">
        {products.length > 0 ? (
          products.map((laptop) => (
            <div className="product-card" key={laptop.id} onClick={() => handleProductClick(laptop.id)}>
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
                <button className="add-to-cart" onClick={() => handleProductClick(laptop.id)}>
                  Chi tiết sản phẩm
                </button>
              </div>
            </div>
          ))
        ) : (
          <div style={{ minHeight: "51px" }}>
            <p>Loading products...</p>
          </div>
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
        <span style={{ marginTop: "10px", padding: "0 20px" }}>
          Trang {currentPage}
        </span>
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
