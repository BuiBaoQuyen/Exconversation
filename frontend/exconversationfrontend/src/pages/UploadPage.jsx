import { useState, useEffect } from 'react';
import { toast } from 'react-toastify';
import { uploadAPI, patternAPI } from '../services/api';
import FileUploader from '../components/Upload/FileUploader';
import UploadList from '../components/Upload/UploadList';
import ParseConfig from '../components/Upload/ParseConfig';
import './UploadPage.css';

function UploadPage() {
  const [uploads, setUploads] = useState([]);
  const [patterns, setPatterns] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedUpload, setSelectedUpload] = useState(null);
  const [showParseConfig, setShowParseConfig] = useState(false);

  useEffect(() => {
    loadUploads();
    loadPatterns();
  }, []);

  const loadUploads = async () => {
    try {
      setLoading(true);
      const response = await uploadAPI.getAll();
      setUploads(response.data);
    } catch (error) {
      toast.error('Failed to load uploads: ' + error.message);
    } finally {
      setLoading(false);
    }
  };

  const loadPatterns = async () => {
    try {
      const response = await patternAPI.getAll();
      const activePatterns = response.data.filter((p) => p.isActive);
      setPatterns(activePatterns);
    } catch (error) {
      toast.error('Failed to load patterns: ' + error.message);
    }
  };

  const handleUpload = async (file, uploadedByName) => {
    try {
      const response = await uploadAPI.upload(file, uploadedByName);
      toast.success('File uploaded successfully');
      loadUploads();
      return response.data;
    } catch (error) {
      toast.error('Failed to upload file: ' + error.message);
      throw error;
    }
  };

  const handleParse = (upload) => {
    setSelectedUpload(upload);
    setShowParseConfig(true);
  };

  const handleParseConfirm = async (patternId) => {
    try {
      await uploadAPI.parse(selectedUpload.id, patternId);
      toast.success('Parsing started. Please check status shortly.');
      setShowParseConfig(false);
      setSelectedUpload(null);
      loadUploads();
    } catch (error) {
      toast.error('Failed to start parsing: ' + error.message);
    }
  };

  const handleParseCancel = () => {
    setShowParseConfig(false);
    setSelectedUpload(null);
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this upload?')) {
      return;
    }
    try {
      await uploadAPI.delete(id);
      toast.success('Upload deleted successfully');
      loadUploads();
    } catch (error) {
      toast.error('Failed to delete upload: ' + error.message);
    }
  };

  if (loading) {
    return <div className="loading">Loading uploads...</div>;
  }

  return (
    <div className="upload-page">
      <div className="page-header">
        <h1>Upload & Parse</h1>
      </div>

      <FileUploader onUpload={handleUpload} />

      {showParseConfig && selectedUpload && (
        <ParseConfig
          upload={selectedUpload}
          patterns={patterns}
          onConfirm={handleParseConfirm}
          onCancel={handleParseCancel}
        />
      )}

      <UploadList
        uploads={uploads}
        onParse={handleParse}
        onDelete={handleDelete}
        onRefresh={loadUploads}
      />
    </div>
  );
}

export default UploadPage;

