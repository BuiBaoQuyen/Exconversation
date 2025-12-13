import { useState, useEffect } from 'react';
import './QuestionForm.css';

function QuestionForm({ question, onSave, onCancel, saving }) {
  const [formData, setFormData] = useState({
    title: '',
    contentLatex: '',
    contentOmml: '',
    chapterId: null,
    type: 'Trắc nghiệm',
    isActive: true,
    answers: [],
  });

  useEffect(() => {
    if (question) {
      console.log('QuestionForm: Setting form data from question:', question);
      console.log('ContentLatex:', question.contentLatex ? `Length: ${question.contentLatex.length}` : 'null/undefined');
      console.log('Answers:', question.answers ? `Count: ${question.answers.length}` : 'null/undefined');
      setFormData({
        title: question.title || '',
        contentLatex: question.contentLatex || '',
        contentOmml: question.contentOmml || '',
        chapterId: question.chapterId || null,
        type: question.type || 'Trắc nghiệm',
        isActive: question.isActive !== undefined ? question.isActive : true,
        answers: question.answers || [],
        currentVersionId: question.currentVersionId,
      });
    }
  }, [question]);

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value,
    }));
  };

  const handleAnswerChange = (index, field, value) => {
    const newAnswers = [...formData.answers];
    newAnswers[index] = {
      ...newAnswers[index],
      [field]: value,
    };
    setFormData((prev) => ({
      ...prev,
      answers: newAnswers,
    }));
  };

  const handleAnswerCorrectChange = (index) => {
    const newAnswers = formData.answers.map((a, i) => ({
      ...a,
      isCorrect: i === index,
    }));
    setFormData((prev) => ({
      ...prev,
      answers: newAnswers,
    }));
  };

  const handleAddAnswer = () => {
    const labels = ['A', 'B', 'C', 'D', 'E', 'F'];
    const nextLabel = labels[formData.answers.length] || String.fromCharCode(65 + formData.answers.length);
    setFormData((prev) => ({
      ...prev,
      answers: [
        ...prev.answers,
        {
          orderLabel: nextLabel,
          contentLatex: '',
          contentOmml: '',
          isCorrect: false,
        },
      ],
    }));
  };

  const handleRemoveAnswer = (index) => {
    const newAnswers = formData.answers.filter((_, i) => i !== index);
    setFormData((prev) => ({
      ...prev,
      answers: newAnswers,
    }));
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    onSave(formData);
  };

  return (
    <form onSubmit={handleSubmit} className="question-form">
      <div className="form-section">
        <h2>Question Details</h2>
        <div className="form-group">
          <label htmlFor="title">Title</label>
          <input
            type="text"
            id="title"
            name="title"
            value={formData.title}
            onChange={handleChange}
            placeholder="e.g., Câu 1: Tính giới hạn..."
          />
        </div>
        <div className="form-group">
          <label htmlFor="contentLatex">Content (LaTeX)</label>
          <textarea
            id="contentLatex"
            name="contentLatex"
            value={formData.contentLatex}
            onChange={handleChange}
            rows="6"
            placeholder="Nội dung LaTeX hiển thị"
          />
          <label htmlFor="contentOmml">Content (OMML cho export DOCX)</label>
          <textarea
            id="contentOmml"
            name="contentOmml"
            value={formData.contentOmml}
            onChange={handleChange}
            rows="6"
            placeholder="OMML (giữ cho xuất DOCX, nếu có)"
          />
        </div>
        <div className="form-group">
          <label htmlFor="type">Type</label>
          <select id="type" name="type" value={formData.type} onChange={handleChange}>
            <option value="Trắc nghiệm">Trắc nghiệm</option>
            <option value="Essay">Essay</option>
            <option value="FillInBlank">Fill In Blank</option>
          </select>
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
      </div>

      <div className="form-section">
        <div className="section-header">
          <h2>Answers</h2>
          <button
            type="button"
            onClick={handleAddAnswer}
            className="btn-secondary"
          >
            Add Answer
          </button>
        </div>
        {formData.answers.length === 0 ? (
          <p className="empty-answers">No answers yet. Click "Add Answer" to add answers.</p>
        ) : (
          <div className="answers-list">
            {formData.answers.map((answer, index) => (
              <div key={index} className="answer-item">
                <div className="answer-header">
                  <label className="answer-label">{answer.orderLabel}</label>
                  <button
                    type="button"
                    onClick={() => handleRemoveAnswer(index)}
                    className="btn-danger btn-small"
                  >
                    Remove
                  </button>
                </div>
                <div className="answer-content">
                  <label>Đáp án (LaTeX)</label>
                  <textarea
                    value={answer.contentLatex || ''}
                    onChange={(e) => handleAnswerChange(index, 'contentLatex', e.target.value)}
                    rows="3"
                    placeholder="Answer LaTeX..."
                  />
                  <label>Đáp án (OMML)</label>
                  <textarea
                    value={answer.contentOmml || ''}
                    onChange={(e) => handleAnswerChange(index, 'contentOmml', e.target.value)}
                    rows="3"
                    placeholder="Answer OMML..."
                  />
                </div>
                <div className="answer-checkbox">
                  <label>
                    <input
                      type="checkbox"
                      checked={answer.isCorrect}
                      onChange={() => handleAnswerCorrectChange(index)}
                    />
                    Correct Answer
                  </label>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      <div className="form-actions">
        <button type="button" onClick={onCancel} className="btn-secondary" disabled={saving}>
          Cancel
        </button>
        <button type="submit" className="btn-primary" disabled={saving}>
          {saving ? 'Saving...' : 'Save Changes'}
        </button>
      </div>
    </form>
  );
}

export default QuestionForm;

