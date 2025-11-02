import { useState, useEffect } from 'react';
import './UploadList.css';

function UploadList({ uploads, onParse, onDelete, onRefresh }) {
  const [autoRefresh, setAutoRefresh] = useState(true);

  useEffect(() => {
    if (autoRefresh) {
      const interval = setInterval(() => {
        onRefresh();
      }, 3000); // Refresh every 3 seconds
      return () => clearInterval(interval);
    }
  }, [autoRefresh, onRefresh]);

  if (uploads.length === 0) {
    return (
      <div className="empty-state">
        <p>No uploads found. Upload a DOCX file to get started.</p>
      </div>
    );
  }

  const getStatusColor = (status) => {
    switch (status) {
      case 'parsed':
        return '#27ae60';
      case 'parsing':
        return '#f39c12';
      case 'error':
        return '#e74c3c';
      default:
        return '#95a5a6';
    }
  };

  const getStatusIcon = (status) => {
    switch (status) {
      case 'parsed':
        return '✓';
      case 'parsing':
        return '⟳';
      case 'error':
        return '✗';
      default:
        return '○';
    }
  };

  return (
    <div className="upload-list">
      <div className="list-header">
        <h2>Upload History</h2>
        <label className="auto-refresh-toggle">
          <input
            type="checkbox"
            checked={autoRefresh}
            onChange={(e) => setAutoRefresh(e.target.checked)}
          />
          Auto-refresh
        </label>
      </div>

      <div className="uploads-grid">
        {uploads.map((upload) => (
          <div key={upload.id} className="upload-card">
            <div className="upload-header">
              <h3>{upload.fileName}</h3>
              <span
                className="status-badge"
                style={{ backgroundColor: getStatusColor(upload.status) }}
              >
                {getStatusIcon(upload.status)} {upload.status}
              </span>
            </div>
            <div className="upload-info">
              <p>
                <strong>Uploaded:</strong>{' '}
                {new Date(upload.uploadDate).toLocaleString()}
              </p>
              {upload.uploadedByName && (
                <p>
                  <strong>By:</strong> {upload.uploadedByName}
                </p>
              )}
              {upload.note && (
                <p>
                  <strong>Note:</strong> {upload.note}
                </p>
              )}
            </div>
            <div className="upload-actions">
              {upload.status === 'pending' && (
                <button
                  onClick={() => onParse(upload)}
                  className="btn-primary"
                >
                  Parse
                </button>
              )}
              <button
                onClick={() => onDelete(upload.id)}
                className="btn-danger"
              >
                Delete
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

export default UploadList;

