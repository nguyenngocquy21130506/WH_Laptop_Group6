import React from "react";
import { useParams } from "react-router-dom";
import a from "./Detail.module.css";

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

function ProductDetail() {
  const { id } = useParams();
  const product = laptopData.find((laptop) => laptop.id === Number(id));

  if (!product) {
    return <h2>Product not found!</h2>;
  }

  return (
    <div style={{display:"flex", justifyContent: "space-around", width:"90%", marginLeft: "5%",}}>
      <div className={a.col}>
        <img src={product.imageUrl} alt={product.name} />
      </div>
      <div className={a.col}>
        <h1>{product.name}</h1>
        <p>Brand: </p>
        <p>Description: </p>
        <p className={a.price}>Price: {product.price}</p>
        {/* Add more details here */}
      </div>
      <div className={a.col}></div>
    </div>
  );
}

export default ProductDetail;
