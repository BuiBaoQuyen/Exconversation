import { useState, useEffect } from 'react';
import { chapterAPI } from '../../services/api';
import './BlueprintForm.css';

function BlueprintForm({ blueprint, onSubmit, onCancel }) {
  const [formData, setFormData] = useState({
    name: '',
    totalQuestions: 40,
    description: '',
    details: [],
  });
  const [chapters, setChapters] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadChapters();
    if (blueprint) {
      setFormData({
        name: blueprint.name || '',
        totalQuestions: blueprint.totalQuestions || 40,
        description: blueprint.description || '',
        details: blueprint.details || [],
      });
    }
  }, [blueprint]);

  const loadChapters = async () => {
    try {
      // Note: Need to create chapter API endpoint
      // For now, use placeholder
      setChapters([]);
    } catch (error) {
      console.error('Failed to load chapters:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: name === 'totalQuestions' ? parseInt(value) || 0 : value,
    }));
  };

  const handleDetailChange = (index, field, value) => {
    const newDetails = [...formData.details];
    newDetails[index] = {
      ...newDetails[index],
      [field]: field === 'percentage' ? parseFloat(value) || 0 : value,
    };
    setFormData((prev) => ({
      ...prev,
      details: newDetails,
    }));
  };

  const handleAddDetail = () => {
    setFormData((prev) => ({
      ...prev,
      details: [
        ...prev.details,
        {
          chapterId: null,
          chapterName: '',
          percentage: 0,
        },
      ],
    }));
  };

  const handleRemoveDetail = (index) => {
    setFormData((prev) => ({
      ...prev,
      details: prev.details.filter((_, i) => i !== index),
    }));
  };

  const calculateTotalPercentage = () => {
    return formData.details.reduce((sum, d) => sum + (d.percentage || 0), 0);
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    const totalPercentage = calculateTotalPercentage();
    if (Math.abs(totalPercentage - 100) > 0.01) {
      alert(`Total percentage must equal 100%. Current: ${totalPercentage}%`);
      return;
    }
    onSubmit(formData);
  };

  if (loading) {
    return <div className="loading">Loading...</div>;
  }

  return (
    <form onSubmit={handleSubmit} className="blueprint-form">
      <div className="form-header">
        <h2>{blueprint ? 'Edit Blueprint' : 'Create Blueprint'}</h2>
        <button type="button" onClick={onCancel} className="btn-close">
          ×
        </button>
      </div>

      <div className="form-group">
        <label htmlFor="name">Blueprint Name *</label>
        <input
          type="text"
          id="name"
          name="name"
          value={formData.name}
          onChange={handleChange}
          required
          placeholder="e.g., Exam 40 Questions"
        />
      </div>

      <div className="form-group">
        <label htmlFor="totalQuestions">Total Questions *</label>
        <input
          type="number"
          id="totalQuestions"
          name="totalQuestions"
          value={formData.totalQuestions}
          onChange={handleChange}
          required
          min="1"
        />
      </div>

      <div className="form-group">
        <label htmlFor="description">Description</label>
        <textarea
          id="description"
          name="description"
          value={formData.description}
          onChange={handleChange}
          rows="3"
          placeholder="Optional description"
        />
      </div>

      <div className="form-section">
        <div className="section-header">
          <h3>Chapter Distribution</h3>
          <button type="button" onClick={handleAddDetail} className="btn-secondary">
            Add Chapter
          </button>
        </div>
        {formData.details.length === 0 ? (
          <p className="empty-message">No chapters added. Total percentage must equal 100%.</p>
        ) : (
          <>
            <div className="details-list">
              {formData.details.map((detail, index) => (
                <div key={index} className="detail-item">
                  <div className="detail-row">
                    <div className="form-group">
                      <label>Chapter ID *</label>
                      <input
                        type="number"
                        value={detail.chapterId || ''}
                        onChange={(e) =>
                          handleDetailChange(index, 'chapterId', parseInt(e.target.value))
                        }
                        required
                        placeholder="Chapter ID"
                      />
                    </div>
                    <div className="form-group">
                      <label>Chapter Name</label>
                      <input
                        type="text"
                        value={detail.chapterName || ''}
                        onChange={(e) =>
                          handleDetailChange(index, 'chapterName', e.target.value)
                        }
                        placeholder="Chapter name (optional)"
                      />
                    </div>
                    <div className="form-group">
                      <label>Percentage *</label>
                      <input
                        type="number"
                        value={detail.percentage || ''}
                        onChange={(e) =>
                          handleDetailChange(index, 'percentage', parseFloat(e.target.value))
                        }
                        required
                        min="0"
                        max="100"
                        step="0.1"
                        placeholder="%"
                      />
                    </div>
                    <button
                      type="button"
                      onClick={() => handleRemoveDetail(index)}
                      className="btn-danger btn-small"
                    >
                      Remove
                    </button>
                  </div>
                </div>
              ))}
            </div>
            <div className="total-percentage">
              <strong>
                Total: {calculateTotalPercentage().toFixed(1)}%
                {Math.abs(calculateTotalPercentage() - 100) > 0.01 && (
                  <span className="error"> (Must equal 100%)</span>
                )}
              </strong>
            </div>
          </>
        )}
      </div>

      <div className="form-actions">
        <button type="button" onClick={onCancel} className="btn-secondary">
          Cancel
        </button>
        <button type="submit" className="btn-primary">
          {blueprint ? 'Update' : 'Create'}
        </button>
      </div>
    </form>
  );
}

export default BlueprintForm;

