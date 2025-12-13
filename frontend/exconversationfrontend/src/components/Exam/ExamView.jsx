import { useState } from 'react';
import MathContentRenderer from '../Question/MathContentRenderer';
import './ExamView.css';

function ExamView({ exam, onExport, onClose }) {
  const [showAnswers, setShowAnswers] = useState(false);

  return (
    <div className="exam-view-overlay">
      <div className="exam-view">
        <div className="view-header">
          <h2>{exam.name}</h2>
          <button onClick={onClose} className="btn-close">
            ×
          </button>
        </div>
        <div className="view-content">
          <div className="exam-meta">
            {exam.questions && (
              <p>
                <strong>Total Questions:</strong> {exam.questions.length}
              </p>
            )}
            <div className="view-actions">
              <label>
                <input
                  type="checkbox"
                  checked={showAnswers}
                  onChange={(e) => setShowAnswers(e.target.checked)}
                />
                Show Answers
              </label>
              <button
                onClick={() => onExport(exam.id, false)}
                className="btn-primary"
              >
                Export (No Answers)
              </button>
              <button
                onClick={() => onExport(exam.id, true)}
                className="btn-primary"
              >
                Export (With Answers)
              </button>
            </div>
          </div>
          {exam.questions && exam.questions.length > 0 ? (
            <div className="questions-list">
              {exam.questions
                .sort((a, b) => a.orderNumber - b.orderNumber)
                .map((eq) => (
                  <div key={eq.id} className="question-item">
                    <div className="question-number">
                      Question {eq.orderNumber}
                    </div>
                    {eq.question && (
                      <>
                        <div className="question-content">
                          <h4>{eq.question.title || `Question ${eq.orderNumber}`}</h4>
                          {eq.question.contentLatex && (
                            <MathContentRenderer 
                              content={eq.question.contentLatex}
                              className="content-text"
                            />
                          )}
                          {showAnswers && eq.question.answers && (
                            <div className="answers-list">
                              <strong>Answers:</strong>
                              <ul>
                                {eq.question.answers.map((answer) => (
                                  <li
                                    key={answer.id}
                                    className={answer.isCorrect ? 'correct' : ''}
                                  >
                                    {answer.orderLabel}. <MathContentRenderer content={answer.contentLatex} />
                                    {answer.isCorrect && ' ✓'}
                                  </li>
                                ))}
                              </ul>
                            </div>
                          )}
                        </div>
                      </>
                    )}
                  </div>
                ))}
            </div>
          ) : (
            <p className="no-questions">No questions in this exam.</p>
          )}
        </div>
      </div>
    </div>
  );
}

export default ExamView;

