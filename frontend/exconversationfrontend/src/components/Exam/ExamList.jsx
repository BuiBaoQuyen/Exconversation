import './ExamList.css';

function ExamList({ exams, onView, onExport, onDelete }) {
  if (exams.length === 0) {
    return (
      <div className="empty-state">
        <p>No exams found.</p>
      </div>
    );
  }

  return (
    <div className="exam-list">
      {exams.map((exam) => (
        <div key={exam.id} className="exam-card">
          <div className="exam-header">
            <h3>{exam.name}</h3>
          </div>
          <div className="exam-info">
            {exam.questions && (
              <p>
                <strong>Questions:</strong> {exam.questions.length}
              </p>
            )}
            {exam.note && (
              <p>
                <strong>Note:</strong> {exam.note}
              </p>
            )}
            <p>
              <strong>Created:</strong>{' '}
              {new Date(exam.createdAt || Date.now()).toLocaleString()}
            </p>
          </div>
          <div className="exam-actions">
            <button onClick={() => onView(exam)} className="btn-primary">
              View
            </button>
            <button
              onClick={() => onExport(exam.id, false)}
              className="btn-secondary"
            >
              Export (No Answers)
            </button>
            <button
              onClick={() => onExport(exam.id, true)}
              className="btn-secondary"
            >
              Export (With Answers)
            </button>
            <button onClick={() => onDelete(exam.id)} className="btn-danger">
              Delete
            </button>
          </div>
        </div>
      ))}
    </div>
  );
}

export default ExamList;

