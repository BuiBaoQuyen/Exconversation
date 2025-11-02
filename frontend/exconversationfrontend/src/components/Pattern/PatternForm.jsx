import { useState, useEffect } from 'react';
import './PatternForm.css';

function PatternForm({ pattern, onSubmit, onCancel }) {
  const [formData, setFormData] = useState({
    patternName: '',
    questionPattern: '',
    answerPattern: '',
    chapterDetector: '',
    exampleText: '',
    isActive: true,
  });

  useEffect(() => {
    if (pattern) {
      setFormData({
        patternName: pattern.patternName || '',
        questionPattern: pattern.questionPattern || '',
        answerPattern: pattern.answerPattern || '',
        chapterDetector: pattern.chapterDetector || '',
        exampleText: pattern.exampleText || '',
        isActive: pattern.isActive !== undefined ? pattern.isActive : true,
      });
    }
  }, [pattern]);

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value,
    }));
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    onSubmit(formData);
  };

  return (
    <div className="pattern-form-container">
      <form onSubmit={handleSubmit} className="pattern-form">
        <h2>{pattern ? 'Edit Pattern' : 'Create New Pattern'}</h2>

        <div className="form-group">
          <label htmlFor="patternName">Pattern Name *</label>
          <input
            type="text"
            id="patternName"
            name="patternName"
            value={formData.patternName}
            onChange={handleChange}
            required
            placeholder="e.g., Default Vietnamese Pattern"
          />
        </div>

        <div className="form-group">
          <label htmlFor="questionPattern">Question Pattern (Regex) *</label>
          <input
            type="text"
            id="questionPattern"
            name="questionPattern"
            value={formData.questionPattern}
            onChange={handleChange}
            required
            placeholder="^Câu\s+(\d+)[:\.]\s*(.+)$"
          />
          <small>Example: ^Câu\s+(\d+)[:\.]\s*(.+)$ matches "Câu 1: ..." or "Câu 1. ..."</small>
        </div>

        <div className="form-group">
          <label htmlFor="answerPattern">Answer Pattern (Regex) *</label>
          <input
            type="text"
            id="answerPattern"
            name="answerPattern"
            value={formData.answerPattern}
            onChange={handleChange}
            required
            placeholder="^\s*([A-D])[\.\)]\s+(.+)$"
          />
          <small>Example: ^\s*([A-D])[\.\)]\s+(.+)$ matches "A. ..." or "A) ..."</small>
        </div>

        <div className="form-group">
          <label htmlFor="chapterDetector">Chapter Detector (Regex)</label>
          <input
            type="text"
            id="chapterDetector"
            name="chapterDetector"
            value={formData.chapterDetector}
            onChange={handleChange}
            placeholder="CHƯƠNG\s+(\d+)"
          />
          <small>Optional: Regex to detect chapter headers</small>
        </div>

        <div className="form-group">
          <label htmlFor="exampleText">Example Text</label>
          <textarea
            id="exampleText"
            name="exampleText"
            value={formData.exampleText}
            onChange={handleChange}
            rows="5"
            placeholder="Sample text to test the pattern"
          />
        </div>

        <div className="form-group checkbox-group">
          <label>
            <input
              type="checkbox"
              name="isActive"
              checked={formData.isActive}
              onChange={handleChange}
            />
            Active
          </label>
        </div>

        <div className="form-actions">
          <button type="button" onClick={onCancel} className="btn-secondary">
            Cancel
          </button>
          <button type="submit" className="btn-primary">
            {pattern ? 'Update' : 'Create'}
          </button>
        </div>
      </form>
    </div>
  );
}

export default PatternForm;

