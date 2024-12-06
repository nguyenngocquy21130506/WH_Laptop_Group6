import React, { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import styles from "./Detail.module.css";
import axios from "axios";

interface Laptop {
  id: number;
  products_name: string;
  price: number;
  brand_name: string;
  short_description: string;
  discount: number;
}

function ProductDetail() {
  const [product, setProduct] = useState<Laptop>();
  const { id } = useParams();

  useEffect(() => {
    const url = `http://localhost:8080/api/productDetail/${id}`;
    axios.post(url).then((response) => {
      setProduct(response.data);
    });
  });

  if (!product) {
    return <h2>Product not found!</h2>;
  }

  return (
    <div
      style={{
        display: "flex",
        justifyContent: "space-around",
        width: "90%",
        marginLeft: "5%",
      }}
    >
      <div className={styles.col}>
        <img src="/imgs/laptop.png" alt="" className={styles.imageProduct} />
      </div>
      <div className={styles.col}>
        <p style={{ fontSize: "18px", fontWeight: "700", color: "red" }}>
          Thương hiệu: {product.brand_name}
        </p>
        <b style={{ fontSize: "20px", color: "#007bff" }}>
          {product.products_name}
        </b>
        <p>Mô tả: {product.short_description}</p>
        <div style={{display:"flex"}}>
          <p className={styles.price}>
            {new Intl.NumberFormat("vi-VN", {
              style: "currency",
              currency: "VND",
            }).format(product.price)}
          </p>
          <p className={styles.discount}>
            {new Intl.NumberFormat("vi-VN", {
              style: "currency",
              currency: "VND",
            }).format(product.price + product.discount)}
          </p>
        </div>
        {/* Add more details here */}
      </div>
      <div className={styles.col}></div>
    </div>
  );
}

export default ProductDetail;
