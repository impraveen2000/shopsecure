import { createContext, useContext, useEffect, useMemo, useState } from 'react';

const CartContext = createContext(null);
const KEY = 'shopsecure-cart';

function load() {
  try {
    return JSON.parse(localStorage.getItem(KEY)) || [];
  } catch {
    return [];
  }
}

// The cart holds no secrets, so localStorage is fine here (unlike tokens).
export function CartProvider({ children }) {
  const [items, setItems] = useState(load);

  useEffect(() => {
    localStorage.setItem(KEY, JSON.stringify(items));
  }, [items]);

  const value = useMemo(() => {
    const add = (product, quantity = 1) =>
      setItems((prev) => {
        const existing = prev.find((i) => i.productId === product.id);
        if (existing) {
          return prev.map((i) =>
            i.productId === product.id ? { ...i, quantity: Math.min(i.quantity + quantity, 100) } : i
          );
        }
        return [...prev, { productId: product.id, name: product.name, price: product.price, quantity }];
      });
    const update = (productId, quantity) =>
      setItems((prev) => prev.map((i) => (i.productId === productId ? { ...i, quantity } : i)));
    const remove = (productId) => setItems((prev) => prev.filter((i) => i.productId !== productId));
    const clear = () => setItems([]);
    const count = items.reduce((n, i) => n + i.quantity, 0);
    const total = items.reduce((sum, i) => sum + Number(i.price) * i.quantity, 0);
    return { items, add, update, remove, clear, count, total };
  }, [items]);

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>;
}

export function useCart() {
  return useContext(CartContext);
}
