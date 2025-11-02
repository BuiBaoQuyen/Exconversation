import { useState, useEffect } from 'react';
import { toast } from 'react-toastify';
import { patternAPI } from '../services/api';
import PatternList from '../components/Pattern/PatternList';
import PatternForm from '../components/Pattern/PatternForm';
import PatternTester from '../components/Pattern/PatternTester';
import './PatternConfigPage.css';

function PatternConfigPage() {
  const [patterns, setPatterns] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedPattern, setSelectedPattern] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [showTester, setShowTester] = useState(false);

  useEffect(() => {
    loadPatterns();
  }, []);

  const loadPatterns = async () => {
    try {
      setLoading(true);
      const response = await patternAPI.getAll();
      setPatterns(response.data);
    } catch (error) {
      toast.error('Failed to load patterns: ' + error.message);
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = () => {
    setSelectedPattern(null);
    setShowForm(true);
    setShowTester(false);
  };

  const handleEdit = (pattern) => {
    setSelectedPattern(pattern);
    setShowForm(true);
    setShowTester(false);
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this pattern?')) {
      return;
    }
    try {
      await patternAPI.delete(id);
      toast.success('Pattern deleted successfully');
      loadPatterns();
    } catch (error) {
      toast.error('Failed to delete pattern: ' + error.message);
    }
  };

  const handleTest = (pattern) => {
    setSelectedPattern(pattern);
    setShowTester(true);
    setShowForm(false);
  };

  const handleFormSubmit = async (data) => {
    try {
      if (selectedPattern) {
        await patternAPI.update(selectedPattern.id, data);
        toast.success('Pattern updated successfully');
      } else {
        await patternAPI.create(data);
        toast.success('Pattern created successfully');
      }
      setShowForm(false);
      setSelectedPattern(null);
      loadPatterns();
    } catch (error) {
      toast.error('Failed to save pattern: ' + error.message);
    }
  };

  const handleFormCancel = () => {
    setShowForm(false);
    setSelectedPattern(null);
  };

  if (loading) {
    return <div className="loading">Loading patterns...</div>;
  }

  return (
    <div className="pattern-config-page">
      <div className="page-header">
        <h1>Pattern Configuration</h1>
        <button onClick={handleCreate} className="btn-primary">
          Create New Pattern
        </button>
      </div>

      {showForm && (
        <PatternForm
          pattern={selectedPattern}
          onSubmit={handleFormSubmit}
          onCancel={handleFormCancel}
        />
      )}

      {showTester && selectedPattern && (
        <PatternTester
          pattern={selectedPattern}
          onClose={() => setShowTester(false)}
        />
      )}

      <PatternList
        patterns={patterns}
        onEdit={handleEdit}
        onDelete={handleDelete}
        onTest={handleTest}
      />
    </div>
  );
}

export default PatternConfigPage;

