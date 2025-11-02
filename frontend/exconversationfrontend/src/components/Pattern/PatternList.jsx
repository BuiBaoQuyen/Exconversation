import './PatternList.css';

function PatternList({ patterns, onEdit, onDelete, onTest }) {
  if (patterns.length === 0) {
    return (
      <div className="empty-state">
        <p>No patterns found. Create your first pattern to get started.</p>
      </div>
    );
  }

  return (
    <div className="pattern-list">
      {patterns.map((pattern) => (
        <div key={pattern.id} className="pattern-card">
          <div className="pattern-header">
            <h3>{pattern.patternName}</h3>
            <div className="pattern-badges">
              {pattern.isActive && <span className="badge active">Active</span>}
            </div>
          </div>
          <div className="pattern-details">
            <div className="pattern-item">
              <label>Question Pattern:</label>
              <code>{pattern.questionPattern}</code>
            </div>
            <div className="pattern-item">
              <label>Answer Pattern:</label>
              <code>{pattern.answerPattern}</code>
            </div>
            {pattern.chapterDetector && (
              <div className="pattern-item">
                <label>Chapter Detector:</label>
                <code>{pattern.chapterDetector}</code>
              </div>
            )}
          </div>
          <div className="pattern-actions">
            <button onClick={() => onTest(pattern)} className="btn-secondary">
              Test
            </button>
            <button onClick={() => onEdit(pattern)} className="btn-secondary">
              Edit
            </button>
            <button onClick={() => onDelete(pattern.id)} className="btn-danger">
              Delete
            </button>
          </div>
        </div>
      ))}
    </div>
  );
}

export default PatternList;

