import Navbar from "./Navbarx";
import Footer from "./Footer";

/**
 * Shared layout for all protected pages.
 * Wraps content with the sticky navbar and the footer.
 * Usage: <Layout><YourPage /></Layout>
 */
export default function Layout({ children }) {
  return (
    <div className="page-layout">
      <Navbar />
      <main className="page-content">
        {children}
      </main>
      <Footer />
    </div>
  );
}