import { useState } from 'react';
import './ParseConfig.css';

function ParseConfig({ upload, patterns, onConfirm, onCancel }) {
  const [selectedPatternId, setSelectedPatternId] = useState(
    patterns.length > 0 ? patterns[0].id : null
  );

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!selectedPatternId) {
      alert('Please select a pattern');
      return;
    }
    onConfirm(selectedPatternId);
  };

  if (patterns.length === 0) {
    return (
      <div className="parse-config">
        <div className="config-header">
          <h2>Parse File: {upload.fileName}</h2>
          <button onClick={onCancel} className="btn-close">×</button>
        </div>
        <div className="config-content">
          <p className="error-message">
            No active patterns found. Please create a pattern first.
          </p>
          <button onClick={onCancel} className="btn-secondary">
            Close
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="parse-config">
      <div className="config-header">
        <h2>Parse File: {upload.fileName}</h2>
        <button onClick={onCancel} className="btn-close">×</button>
      </div>
      <form onSubmit={handleSubmit} className="config-content">
        <div className="form-group">
          <label htmlFor="pattern">Select Pattern *</label>
          <select
            id="pattern"
            value={selectedPatternId || ''}
            onChange={(e) => setSelectedPatternId(Number(e.target.value))}
            required
          >
            <option value="">-- Select a pattern --</option>
            {patterns.map((pattern) => (
              <option key={pattern.id} value={pattern.id}>
                {pattern.patternName}
              </option>
            ))}
          </select>
          {selectedPatternId && (
            <div className="pattern-preview">
              {patterns
                .find((p) => p.id === selectedPatternId)
                ?.questionPattern && (
                <div className="preview-item">
                  <strong>Question Pattern:</strong>
                  <code>
                    {
                      patterns.find((p) => p.id === selectedPatternId)
                        ?.questionPattern
                    }
                  </code>
                </div>
              )}
            </div>
          )}
        </div>
        <div className="form-actions">
          <button type="button" onClick={onCancel} className="btn-secondary">
            Cancel
          </button>
          <button type="submit" className="btn-primary">
            Start Parsing
          </button>
        </div>
      </form>
    </div>
  );
}

export default ParseConfig;

