import { useState } from 'react';
import './ExamGenerator.css';

function ExamGenerator({ blueprint, onGenerate, onCancel }) {
  const [examName, setExamName] = useState('');
  const [generating, setGenerating] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!examName.trim()) {
      alert('Please enter exam name');
      return;
    }
    try {
      setGenerating(true);
      await onGenerate(blueprint.id, examName);
    } finally {
      setGenerating(false);
    }
  };

  return (
    <div className="exam-generator-overlay">
      <div className="exam-generator">
        <div className="generator-header">
          <h2>Generate Exam</h2>
          <button onClick={onCancel} className="btn-close">
            ×
          </button>
        </div>
        <div className="generator-content">
          <div className="blueprint-info">
            <h3>{blueprint.name}</h3>
            <p>
              <strong>Total Questions:</strong> {blueprint.totalQuestions}
            </p>
            {blueprint.details && blueprint.details.length > 0 && (
              <div className="distribution">
                <strong>Distribution:</strong>
                <ul>
                  {blueprint.details.map((detail) => (
                    <li key={detail.id}>
                      {detail.chapterName}: {detail.percentage}%
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </div>
          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label htmlFor="examName">Exam Name *</label>
              <input
                type="text"
                id="examName"
                value={examName}
                onChange={(e) => setExamName(e.target.value)}
                required
                placeholder="e.g., Exam 1 - Midterm"
              />
            </div>
            <div className="form-actions">
              <button type="button" onClick={onCancel} className="btn-secondary" disabled={generating}>
                Cancel
              </button>
              <button type="submit" className="btn-primary" disabled={generating}>
                {generating ? 'Generating...' : 'Generate Exam'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}

export default ExamGenerator;

