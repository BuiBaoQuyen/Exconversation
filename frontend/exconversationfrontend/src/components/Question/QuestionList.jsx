import QuestionCard from './QuestionCard';
import './QuestionList.css';

function QuestionList({ questions, groupedQuestions, onEdit, onDelete, onUpdate }) {
  if (questions.length === 0) {
    return (
      <div className="empty-state">
        <p>No questions found. Parse a DOCX file first.</p>
      </div>
    );
  }

  // If groupedQuestions is provided, display grouped by section/chapter
  if (groupedQuestions && Object.keys(groupedQuestions).length > 0) {
    return (
      <div className="question-list-grouped">
        {Object.entries(groupedQuestions).map(([groupKey, group]) => (
          <div key={groupKey} className="question-group">
            {(group.section || group.chapter) && (
              <div className="group-header">
                {group.section && group.chapter && (
                  <h2 className="section-header">
                    {group.section} - {group.chapter}
                  </h2>
                )}
                {group.section && !group.chapter && (
                  <h2 className="section-header">{group.section}</h2>
                )}
                {group.chapter && !group.section && (
                  <h2 className="chapter-header">{group.chapter}</h2>
                )}
              </div>
            )}
            <div className="questions-in-group">
              {group.questions.map((question) => (
                <QuestionCard
                  key={question.id}
                  question={question}
                  onUpdate={onUpdate}
                  onDelete={onDelete}
                />
              ))}
            </div>
          </div>
        ))}
      </div>
    );
  }

  // Fallback to flat list
  return (
    <div className="question-list">
      {questions.map((question) => (
        <QuestionCard
          key={question.id}
          question={question}
          onUpdate={onUpdate}
          onDelete={onDelete}
        />
      ))}
    </div>
  );
}

export default QuestionList;

