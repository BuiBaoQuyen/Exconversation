import { useState } from 'react';
import { toast } from 'react-toastify';
import { patternAPI } from '../../services/api';
import './PatternTester.css';

function PatternTester({ pattern, onClose }) {
  const [testText, setTestText] = useState(pattern.exampleText || '');
  const [testResult, setTestResult] = useState(null);
  const [loading, setLoading] = useState(false);

  const handleTest = async () => {
    if (!testText.trim()) {
      toast.warning('Please enter test text');
      return;
    }

    try {
      setLoading(true);
      const response = await patternAPI.test(pattern.id, testText);
      setTestResult(response.data);
      if (response.data.success) {
        toast.success(response.data.message);
      } else {
        toast.error(response.data.message);
      }
    } catch (error) {
      toast.error('Failed to test pattern: ' + error.message);
      setTestResult(null);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="pattern-tester-container">
      <div className="pattern-tester">
        <div className="tester-header">
          <h2>Test Pattern: {pattern.patternName}</h2>
          <button onClick={onClose} className="btn-close">×</button>
        </div>

        <div className="tester-content">
          <div className="form-group">
            <label htmlFor="testText">Test Text</label>
            <textarea
              id="testText"
              value={testText}
              onChange={(e) => setTestText(e.target.value)}
              rows="10"
              placeholder="Paste sample text here to test the pattern..."
            />
            <button
              onClick={handleTest}
              disabled={loading}
              className="btn-primary"
              style={{ marginTop: '0.5rem' }}
            >
              {loading ? 'Testing...' : 'Test Pattern'}
            </button>
          </div>

          {testResult && (
            <div className="test-results">
              <h3>Test Results</h3>
              <div className="result-item">
                <strong>Status:</strong>{' '}
                <span className={testResult.success ? 'success' : 'error'}>
                  {testResult.success ? '✓ Success' : '✗ Failed'}
                </span>
              </div>
              <div className="result-item">
                <strong>Message:</strong> {testResult.message}
              </div>

              {testResult.matchedQuestions && testResult.matchedQuestions.length > 0 && (
                <div className="result-item">
                  <strong>Matched Questions ({testResult.matchedQuestions.length}):</strong>
                  <ul>
                    {testResult.matchedQuestions.map((q, idx) => (
                      <li key={idx}>
                        <code>{q}</code>
                      </li>
                    ))}
                  </ul>
                </div>
              )}

              {testResult.matchedAnswers && testResult.matchedAnswers.length > 0 && (
                <div className="result-item">
                  <strong>Matched Answers ({testResult.matchedAnswers.length}):</strong>
                  <ul>
                    {testResult.matchedAnswers.map((a, idx) => (
                      <li key={idx}>
                        <code>{a}</code>
                      </li>
                    ))}
                  </ul>
                </div>
              )}

              {testResult.matchedChapters && testResult.matchedChapters.length > 0 && (
                <div className="result-item">
                  <strong>Matched Chapters ({testResult.matchedChapters.length}):</strong>
                  <ul>
                    {testResult.matchedChapters.map((c, idx) => (
                      <li key={idx}>
                        <code>{c}</code>
                      </li>
                    ))}
                  </ul>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export default PatternTester;

