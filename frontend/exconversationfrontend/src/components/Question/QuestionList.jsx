import { Link } from 'react-router-dom';
import './QuestionList.css';

function QuestionList({ questions, onEdit, onDelete }) {
  if (questions.length === 0) {
    return (
      <div className="empty-state">
        <p>No questions found. Parse a DOCX file first.</p>
      </div>
    );
  }

  return (
    <div className="question-list">
      {questions.map((question) => (
        <div key={question.id} className="question-card">
          <div className="question-header">
            <div>
              <h3>{question.title || `Question #${question.id}`}</h3>
              {question.chapterName && (
                <span className="chapter-badge">{question.chapterName}</span>
              )}
            </div>
            <span className={`status-badge ${question.isActive ? 'active' : 'inactive'}`}>
              {question.isActive ? 'Active' : 'Inactive'}
            </span>
          </div>
          <div className="question-preview">
            {question.content && (
              <div
                className="content-preview"
                dangerouslySetInnerHTML={{
                  __html: question.content.substring(0, 200) + '...',
                }}
              />
            )}
          </div>
          {question.answers && question.answers.length > 0 && (
            <div className="question-answers">
              <strong>Answers:</strong>{' '}
              {question.answers.map((a) => (
                <span key={a.id} className={`answer-label ${a.isCorrect ? 'correct' : ''}`}>
                  {a.orderLabel}
                </span>
              ))}
            </div>
          )}
          <div className="question-actions">
            <Link to={`/questions/${question.id}/edit`} className="btn-primary">
              Edit
            </Link>
            <button onClick={() => onDelete(question.id)} className="btn-danger">
              Delete
            </button>
          </div>
        </div>
      ))}
    </div>
  );
}

export default QuestionList;

