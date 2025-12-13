import { Link } from 'react-router-dom';
import './Dashboard.css';

function Dashboard() {
  return (
    <div className="dashboard">
      <h1>Dashboard</h1>
      <div className="dashboard-grid">
        {/* Pattern configuration removed - using AI detection only */}
        {/* <Link to="/patterns" className="dashboard-card">
          <h2>Pattern Configuration</h2>
          <p>Manage question detection patterns</p>
        </Link> */}
        <Link to="/upload" className="dashboard-card">
          <h2>Upload & Parse</h2>
          <p>Upload DOCX files. AI automatically detects questions, answers, and chapters.</p>
        </Link>
        <Link to="/questions" className="dashboard-card">
          <h2>Question Management</h2>
          <p>View and edit questions</p>
        </Link>
        <Link to="/exams" className="dashboard-card">
          <h2>Exam Generation</h2>
          <p>Create and generate exams</p>
        </Link>
      </div>
    </div>
  );
}

export default Dashboard;

