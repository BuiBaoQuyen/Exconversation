import { Outlet, Link, useLocation } from 'react-router-dom';
import './Layout.css';

function Layout() {
  const location = useLocation();

  const isActive = (path) => location.pathname === path;

  return (
    <div className="layout">
      <header className="header">
        <div className="header-content">
          <h1 className="logo">ExConversation</h1>
          <nav className="nav">
            <Link to="/" className={isActive('/') ? 'active' : ''}>
              Dashboard
            </Link>
            {/* Pattern configuration removed - using AI detection only */}
            {/* <Link to="/patterns" className={isActive('/patterns') ? 'active' : ''}>
              Patterns
            </Link> */}
            <Link to="/upload" className={isActive('/upload') ? 'active' : ''}>
              Upload
            </Link>
            <Link to="/questions" className={isActive('/questions') ? 'active' : ''}>
              Questions
            </Link>
            <Link to="/exams" className={isActive('/exams') ? 'active' : ''}>
              Exams
            </Link>
          </nav>
        </div>
      </header>
      <main className="main-content">
        <Outlet />
      </main>
    </div>
  );
}

export default Layout;

