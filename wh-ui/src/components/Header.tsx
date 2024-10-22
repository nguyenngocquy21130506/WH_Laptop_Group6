import React from "react";
import { Link } from "react-router-dom";
import "./Header.css";

function Header() {
  return (
    <div
      style={{
        width: "100%",
        height: "300px",
        backgroundColor: "#ccc",
      }}
    >
      <div
        style={{
          width: "30%",
          position: "absolute",
          color: "white",
          alignContent: "center",
          justifyContent: "center",
        }}
      >
        <h1 style={{ fontStyle: "italic", fontFamily: "DynaPuff" }}>Team6</h1>
      </div>
      <div
        style={{ width: "30%", backgroundColor:'black', height: "100%" }}
      ></div>
      <img
        src="/imgs/laptop.png"
        alt="laptop"
        style={{width:'250px', height:'250px', position: "absolute", top: 35, left: 50 }}
      />
      <header className="header">
        <nav>
          <ul>
            <li>
              <Link to="/">Home</Link>
            </li>
            <li>
              <Link to="/contact">Contact</Link>
            </li>
          </ul>
        </nav>
      </header>
      <div style={{position:'absolute', right:'50%', top:'20%'}}>
        <ul style={{listStyle:'none'}}>
          <li className="memberName">21130_Tiến</li>
          <li className="memberName">21130_Quý</li>
          <li className="memberName">21130_Ngọc</li>
          <li className="memberName">21130_Bel</li>
        </ul>
      </div>
    </div>
  );
}

export default Header;
